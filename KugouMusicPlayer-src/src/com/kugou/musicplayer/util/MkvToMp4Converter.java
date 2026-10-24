package com.kugou.musicplayer.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * MKV到MP4转封装器（纯Java，不解码，只换容器）
 *
 * 支持：H.264视频 + AAC音频
 * 将MKV中的H.264(Annex B)和AAC流提取出来，重新封装为MP4
 * 这样Android原生MediaPlayer就能播放了
 *
 * 注意：这是简化实现，只支持最常见的MKV结构
 */
public class MkvToMp4Converter {

    // MKV Element IDs
    private static final long ID_EBML = 0x1A45DFA3L;
    private static final long ID_SEGMENT = 0x18538067L;
    private static final long ID_SEEK_HEAD = 0x114D9B74L;
    private static final long ID_INFO = 0x1549A966L;
    private static final long ID_TIMECODE_SCALE = 0x2AD7B1L;
    private static final long ID_DURATION = 0x4489L;
    private static final long ID_TRACKS = 0x1654AE6BL;
    private static final long ID_TRACK_ENTRY = 0xAE;
    private static final long ID_TRACK_NUMBER = 0xD7;
    private static final long ID_TRACK_TYPE = 0x83;
    private static final long ID_CODEC_ID = 0x86;
    private static final long ID_CODEC_PRIVATE = 0x63A2;
    private static final long ID_VIDEO = 0xE0;
    private static final long ID_PIXEL_WIDTH = 0xB0;
    private static final long ID_PIXEL_HEIGHT = 0xBA;
    private static final long ID_AUDIO = 0xE1;
    private static final long ID_SAMPLING_FREQ = 0xB5;
    private static final long ID_CHANNELS = 0x9F;
    private static final long ID_CLUSTER = 0x1F43B675L;
    private static final long ID_TIMECODE = 0xE7;
    private static final long ID_SIMPLE_BLOCK = 0xA3;
    private static final long ID_BLOCK_GROUP = 0xA0;
    private static final long ID_BLOCK = 0xA1;
    private static final long ID_CUES = 0x1C53BB6BL;

    private byte[] data;
    private int pos;

    // 轨道信息
    private int videoTrackNum = -1;
    private int audioTrackNum = -1;
    private String videoCodec = "";
    private String audioCodec = "";
    private byte[] videoCodecPrivate; // H.264 AVCC
    private byte[] audioCodecPrivate; // AAC config
    private int videoWidth = 0;
    private int videoHeight = 0;
    private int audioSampleRate = 44100;
    private int audioChannels = 2;
    private long timecodeScale = 1000000; // 默认1ms
    private double duration = 0;

    // 帧数据
    private List videoFrames = new ArrayList(); // long[] {timestamp, size, offset}
    private List audioFrames = new ArrayList();

    /**
     * 转换MKV文件为MP4文件
     * @param mkvPath MKV文件路径
     * @param mp4Path 输出MP4文件路径
     * @return 是否成功
     */
    public boolean convert(String mkvPath, String mp4Path) {
        try {
            // 读取整个文件
            File file = new File(mkvPath);
            if (!file.exists() || file.length() == 0) {
                return false;
            }
            data = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            int read = 0;
            while (read < data.length) {
                int r = fis.read(data, read, data.length - read);
                if (r <= 0) break;
                read += r;
            }
            fis.close();
            pos = 0;

            // 解析
            parse();

            // 检查是否有视频轨道
            if (videoTrackNum < 0 && audioTrackNum < 0) {
                return false;
            }

            // 写入MP4
            writeMp4(mp4Path);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 解析MKV文件
     */
    private void parse() {
        while (pos < data.length - 4) {
            long id = readElementId();
            long size = readElementSize();
            if (size <= 0 || pos + size > data.length) break;

            if (id == ID_EBML) {
                // 跳过EBML头
                pos += (int) size;
            } else if (id == ID_SEGMENT) {
                parseSegment((int) size);
                break;
            } else {
                pos += (int) size;
            }
        }
    }

    /**
     * 解析Segment
     */
    private void parseSegment(int segmentSize) {
        int end = pos + segmentSize;
        while (pos < end) {
            long id = readElementId();
            long size = readElementSize();
            if (size <= 0 || pos + size > end) break;

            if (id == ID_INFO) {
                parseInfo((int) size);
            } else if (id == ID_TRACKS) {
                parseTracks((int) size);
            } else if (id == ID_CLUSTER) {
                parseCluster((int) size);
            } else if (id == ID_CUES) {
                pos += (int) size; // 跳过索引
            } else {
                pos += (int) size;
            }
        }
    }

    /**
     * 解析Info
     */
    private void parseInfo(int size) {
        int end = pos + size;
        while (pos < end) {
            long id = readElementId();
            long s = readElementSize();
            if (s <= 0 || pos + s > end) break;

            if (id == ID_TIMECODE_SCALE) {
                timecodeScale = readUint((int) s);
            } else if (id == ID_DURATION) {
                duration = readFloat((int) s);
            } else {
                pos += (int) s;
            }
        }
    }

    /**
     * 解析Tracks
     */
    private void parseTracks(int size) {
        int end = pos + size;
        while (pos < end) {
            long id = readElementId();
            long s = readElementSize();
            if (s <= 0 || pos + s > end) break;

            if (id == ID_TRACK_ENTRY) {
                parseTrackEntry((int) s);
            } else {
                pos += (int) s;
            }
        }
    }

    /**
     * 解析单个TrackEntry
     */
    private void parseTrackEntry(int size) {
        int end = pos + size;
        int trackNum = -1;
        int trackType = -1;
        String codecId = "";
        byte[] codecPrivate = null;
        int width = 0, height = 0;
        int sampleRate = 44100, channels = 2;

        while (pos < end) {
            long id = readElementId();
            long s = readElementSize();
            if (s <= 0 || pos + s > end) break;

            if (id == ID_TRACK_NUMBER) {
                trackNum = (int) readUint((int) s);
            } else if (id == ID_TRACK_TYPE) {
                trackType = (int) readUint((int) s);
            } else if (id == ID_CODEC_ID) {
                codecId = readString((int) s);
            } else if (id == ID_CODEC_PRIVATE) {
                codecPrivate = new byte[(int) s];
                System.arraycopy(data, pos, codecPrivate, 0, (int) s);
                pos += (int) s;
            } else if (id == ID_VIDEO) {
                int vend = pos + (int) s;
                while (pos < vend) {
                    long vid = readElementId();
                    long vs = readElementSize();
                    if (vs <= 0 || pos + vs > vend) break;
                    if (vid == ID_PIXEL_WIDTH) width = (int) readUint((int) vs);
                    else if (vid == ID_PIXEL_HEIGHT) height = (int) readUint((int) vs);
                    else pos += (int) vs;
                }
            } else if (id == ID_AUDIO) {
                int aend = pos + (int) s;
                while (pos < aend) {
                    long aid = readElementId();
                    long as = readElementSize();
                    if (as <= 0 || pos + as > aend) break;
                    if (aid == ID_SAMPLING_FREQ) sampleRate = (int) readFloat((int) as);
                    else if (aid == ID_CHANNELS) channels = (int) readUint((int) as);
                    else pos += (int) as;
                }
            } else {
                pos += (int) s;
            }
        }

        if (trackType == 1 && codecId.contains("AVC")) {
            videoTrackNum = trackNum;
            videoCodec = codecId;
            videoCodecPrivate = codecPrivate;
            videoWidth = width;
            videoHeight = height;
        } else if (trackType == 2 && codecId.contains("AAC")) {
            audioTrackNum = trackNum;
            audioCodec = codecId;
            audioCodecPrivate = codecPrivate;
            audioSampleRate = sampleRate;
            audioChannels = channels;
        }
    }

    /**
     * 解析Cluster
     */
    private void parseCluster(int size) {
        int end = pos + size;
        long clusterTimecode = 0;

        while (pos < end) {
            long id = readElementId();
            long s = readElementSize();
            if (s <= 0 || pos + s > end) break;

            if (id == ID_TIMECODE) {
                clusterTimecode = readUint((int) s);
            } else if (id == ID_SIMPLE_BLOCK) {
                parseSimpleBlock((int) s, clusterTimecode);
            } else if (id == ID_BLOCK_GROUP) {
                int bend = pos + (int) s;
                while (pos < bend) {
                    long bid = readElementId();
                    long bs = readElementSize();
                    if (bs <= 0 || pos + bs > bend) break;
                    if (bid == ID_BLOCK) {
                        parseBlock((int) bs, clusterTimecode, false);
                    } else {
                        pos += (int) bs;
                    }
                }
            } else {
                pos += (int) s;
            }
        }
    }

    /**
     * 解析SimpleBlock
     */
    private void parseSimpleBlock(int size, long clusterTimecode) {
        int start = pos;
        int trackNum = (int) readVint();
        short timecode = (short) ((data[pos] << 8) | (data[pos + 1] & 0xFF));
        pos += 2;
        int flags = data[pos] & 0xFF;
        pos++;
        boolean keyframe = (flags & 0x80) != 0;

        int dataSize = size - (pos - start);
        long pts = (clusterTimecode + timecode) * timecodeScale / 1000; // 转ms

        if (trackNum == videoTrackNum) {
            videoFrames.add(new long[]{pts, dataSize, pos, keyframe ? 1 : 0});
        } else if (trackNum == audioTrackNum) {
            audioFrames.add(new long[]{pts, dataSize, pos, 1});
        }
        pos += dataSize;
    }

    /**
     * 解析Block（BlockGroup中的）
     */
    private void parseBlock(int size, long clusterTimecode, boolean keyframe) {
        int start = pos;
        int trackNum = (int) readVint();
        short timecode = (short) ((data[pos] << 8) | (data[pos + 1] & 0xFF));
        pos += 2;
        pos++; // flags

        int dataSize = size - (pos - start);
        long pts = (clusterTimecode + timecode) * timecodeScale / 1000;

        if (trackNum == videoTrackNum) {
            videoFrames.add(new long[]{pts, dataSize, pos, keyframe ? 1 : 0});
        } else if (trackNum == audioTrackNum) {
            audioFrames.add(new long[]{pts, dataSize, pos, 1});
        }
        pos += dataSize;
    }

    // ========== EBML读取工具 ==========

    private long readElementId() {
        // Element ID包含标记位（与Element Size不同）
        int firstByte = data[pos] & 0xFF;
        int length = 1;
        long mask = 0x80;
        while ((firstByte & mask) == 0) {
            length++;
            mask >>= 1;
            if (length > 4) return -1; // ID最多4字节
        }
        long value = firstByte; // 保留标记位
        pos++;
        for (int i = 1; i < length; i++) {
            value = (value << 8) | (data[pos] & 0xFF);
            pos++;
        }
        return value;
    }

    private long readElementSize() {
        return readVint();
    }

    private long readVint() {
        int firstByte = data[pos] & 0xFF;
        int length = 1;
        long mask = 0x80;
        while ((firstByte & mask) == 0) {
            length++;
            mask >>= 1;
            if (length > 8) return -1;
        }
        long value = firstByte & (mask - 1); // 去掉标记位
        pos++;
        for (int i = 1; i < length; i++) {
            value = (value << 8) | (data[pos] & 0xFF);
            pos++;
        }
        return value;
    }

    private long readUint(int size) {
        long value = 0;
        for (int i = 0; i < size; i++) {
            value = (value << 8) | (data[pos] & 0xFF);
            pos++;
        }
        return value;
    }

    private double readFloat(int size) {
        if (size == 4) {
            int bits = 0;
            for (int i = 0; i < 4; i++) {
                bits = (bits << 8) | (data[pos] & 0xFF);
                pos++;
            }
            return Float.intBitsToFloat(bits);
        } else if (size == 8) {
            long bits = 0;
            for (int i = 0; i < 8; i++) {
                bits = (bits << 8) | (data[pos] & 0xFF);
                pos++;
            }
            return Double.longBitsToDouble(bits);
        } else {
            pos += size;
            return 0;
        }
    }

    private String readString(int size) {
        String s = new String(data, pos, size);
        pos += size;
        return s;
    }

    // ========== MP4写入 ==========

    private void writeMp4(String mp4Path) throws IOException {
        // 计算时间尺度
        int videoTimescale = 1000; // 使用ms
        int audioTimescale = audioSampleRate;

        // ftyp
        byte[] ftypBox = box("ftyp", concat(
                bytes("isom"),
                new byte[]{0, 0, 0, 0},
                bytes("isom"),
                bytes("mp41")
        ));

        // 收集视频帧
        List videoSampleSizes = new ArrayList();
        List videoSampleTimes = new ArrayList();
        ByteArrayOutputStream videoData = new ByteArrayOutputStream();
        long videoDuration = 0;
        long lastVideoPts = -1;

        for (int i = 0; i < videoFrames.size(); i++) {
            long[] frame = (long[]) videoFrames.get(i);
            long pts = frame[0];
            int fsize = (int) frame[1];
            int foffset = (int) frame[2];

            byte[] avccFrame = annexBToAvcc(foffset, fsize);
            if (avccFrame != null && avccFrame.length > 0) {
                videoData.write(avccFrame);
                videoSampleSizes.add(avccFrame.length);

                long duration;
                if (lastVideoPts >= 0) {
                    duration = pts - lastVideoPts;
                    if (duration <= 0 || duration > 1000) duration = 42;
                } else {
                    duration = 42;
                }
                videoSampleTimes.add(duration);
                videoDuration += duration;
                lastVideoPts = pts;
            }
        }

        // 收集音频帧（仅AAC）
        List audioSampleSizes = new ArrayList();
        List audioSampleTimes = new ArrayList();
        ByteArrayOutputStream audioData = new ByteArrayOutputStream();
        long audioDuration = 0;
        long lastAudioPts = -1;
        boolean audioSupported = audioCodec.contains("AAC");

        if (audioSupported) {
            for (int i = 0; i < audioFrames.size(); i++) {
                long[] frame = (long[]) audioFrames.get(i);
                long pts = frame[0];
                int fsize = (int) frame[1];
                int foffset = (int) frame[2];

                byte[] rawAac = extractAacRaw(foffset, fsize);
                if (rawAac != null && rawAac.length > 0) {
                    audioData.write(rawAac);
                    audioSampleSizes.add(rawAac.length);
                    long duration;
                    if (lastAudioPts >= 0) {
                        duration = pts - lastAudioPts;
                        if (duration <= 0 || duration > 1000) duration = 23;
                    } else {
                        duration = 23;
                    }
                    audioSampleTimes.add(duration * audioSampleRate / 1000);
                    audioDuration += duration * audioSampleRate / 1000;
                    lastAudioPts = pts;
                }
            }
        }

        // mdat数据
        ByteArrayOutputStream mdatData = new ByteArrayOutputStream();
        mdatData.write(videoData.toByteArray());
        if (audioSupported) {
            mdatData.write(audioData.toByteArray());
        }
        byte[] mdatContent = mdatData.toByteArray();

        // 第一次构建moov（偏移用0占位），计算moov大小
        long totalDuration = Math.max(videoDuration,
                audioDuration > 0 ? audioDuration * 1000 / audioSampleRate : 0);

        ByteArrayOutputStream moov1 = new ByteArrayOutputStream();
        moov1.write(box("mvhd", createMvhd(totalDuration, videoTimescale)));
        if (videoTrackNum >= 0 && videoFrames.size() > 0) {
            moov1.write(box("trak", createVideoTrak(
                    videoSampleSizes, videoSampleTimes, 0,
                    videoWidth, videoHeight, videoCodecPrivate, videoTimescale)));
        }
        if (audioSupported && audioTrackNum >= 0 && audioFrames.size() > 0) {
            moov1.write(box("trak", createAudioTrak(
                    audioSampleSizes, audioSampleTimes, 0,
                    audioSampleRate, audioChannels, audioCodecPrivate, audioTimescale)));
        }
        byte[] moovBox1 = box("moov", moov1.toByteArray());

        // 计算实际偏移：ftyp + moov + mdat header(8)
        long mdatDataStart = ftypBox.length + moovBox1.length + 8;
        long videoChunkOffset = mdatDataStart;
        long audioChunkOffset = mdatDataStart + videoData.size();

        // 第二次构建moov（使用正确偏移）
        ByteArrayOutputStream moov2 = new ByteArrayOutputStream();
        moov2.write(box("mvhd", createMvhd(totalDuration, videoTimescale)));
        if (videoTrackNum >= 0 && videoFrames.size() > 0) {
            moov2.write(box("trak", createVideoTrak(
                    videoSampleSizes, videoSampleTimes, videoChunkOffset,
                    videoWidth, videoHeight, videoCodecPrivate, videoTimescale)));
        }
        if (audioSupported && audioTrackNum >= 0 && audioFrames.size() > 0) {
            moov2.write(box("trak", createAudioTrak(
                    audioSampleSizes, audioSampleTimes, audioChunkOffset,
                    audioSampleRate, audioChannels, audioCodecPrivate, audioTimescale)));
        }
        byte[] moovBox2 = box("moov", moov2.toByteArray());

        // 验证两次moov大小相同
        if (moovBox1.length != moovBox2.length) {
            // 大小不同，需要重新计算偏移（通常不会发生）
            mdatDataStart = ftypBox.length + moovBox2.length + 8;
            videoChunkOffset = mdatDataStart;
            audioChunkOffset = mdatDataStart + videoData.size();
            // 第三次构建
            ByteArrayOutputStream moov3 = new ByteArrayOutputStream();
            moov3.write(box("mvhd", createMvhd(totalDuration, videoTimescale)));
            if (videoTrackNum >= 0 && videoFrames.size() > 0) {
                moov3.write(box("trak", createVideoTrak(
                        videoSampleSizes, videoSampleTimes, videoChunkOffset,
                        videoWidth, videoHeight, videoCodecPrivate, videoTimescale)));
            }
            if (audioSupported && audioTrackNum >= 0 && audioFrames.size() > 0) {
                moov3.write(box("trak", createAudioTrak(
                        audioSampleSizes, audioSampleTimes, audioChunkOffset,
                        audioSampleRate, audioChannels, audioCodecPrivate, audioTimescale)));
            }
            moovBox2 = box("moov", moov3.toByteArray());
        }

        // 写入文件：ftyp + moov + mdat
        FileOutputStream fos = new FileOutputStream(mp4Path);
        fos.write(ftypBox);
        fos.write(moovBox2);
        fos.write(box("mdat", mdatContent));
        fos.close();
    }

    /**
     * Annex B格式转AVCC格式
     * Annex B: 00 00 00 01 NALU 00 00 00 01 NALU...
     * AVCC: [4字节长度] NALU [4字节长度] NALU...
     * 返回完整的AVCC帧数据（每个NALU都带4字节长度前缀）
     */
    private byte[] annexBToAvcc(int offset, int size) {
        try {
            // 找到所有起始码位置
            List nalus = new ArrayList();
            int i = 0;
            while (i < size - 3) {
                if (data[offset + i] == 0 && data[offset + i + 1] == 0) {
                    if (data[offset + i + 2] == 1) {
                        nalus.add(new int[]{i + 3, -1});
                        i += 3;
                    } else if (data[offset + i + 2] == 0 && i + 3 < size && data[offset + i + 3] == 1) {
                        nalus.add(new int[]{i + 4, -1});
                        i += 4;
                    } else {
                        i++;
                    }
                } else {
                    i++;
                }
            }
            if (nalus.isEmpty()) {
                // 没有起始码，可能已经是AVCC格式，直接返回
                byte[] result = new byte[size];
                System.arraycopy(data, offset, result, 0, size);
                return result;
            }

            // 设置每个NALU的结束位置
            for (int j = 0; j < nalus.size(); j++) {
                int[] nalu = (int[]) nalus.get(j);
                if (j + 1 < nalus.size()) {
                    int[] next = (int[]) nalus.get(j + 1);
                    int end = next[0];
                    // 回退到起始码前（去掉trailing zeros）
                    while (end > nalu[0] + 3) {
                        if (data[offset + end - 1] == 0 && data[offset + end - 2] == 0
                                && data[offset + end - 3] == 0) {
                            end--;
                        } else {
                            break;
                        }
                    }
                    nalu[1] = end;
                } else {
                    nalu[1] = size;
                }
            }

            // 构建AVCC格式：每个NALU前加4字节长度
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (int j = 0; j < nalus.size(); j++) {
                int[] nalu = (int[]) nalus.get(j);
                int naluSize = nalu[1] - nalu[0];
                if (naluSize <= 0) continue;
                // 跳过SPS(7)和PPS(8)，它们在avcC中
                int nalType = data[offset + nalu[0]] & 0x1F;
                if (nalType == 7 || nalType == 8) continue;
                // 写4字节长度（大端）
                out.write((naluSize >> 24) & 0xFF);
                out.write((naluSize >> 16) & 0xFF);
                out.write((naluSize >> 8) & 0xFF);
                out.write(naluSize & 0xFF);
                // 写NALU数据
                out.write(data, offset + nalu[0], naluSize);
            }
            byte[] result = out.toByteArray();
            return result.length > 0 ? result : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从ADTS格式中提取AAC raw数据
     */
    private byte[] extractAacRaw(int offset, int size) {
        try {
            if (size < 7) return null;
            // 检查是否是ADTS头
            if ((data[offset] & 0xFF) == 0xFF && (data[offset + 1] & 0xF0) == 0xF0) {
                // ADTS头长度：7或9字节
                int headerLen = ((data[offset + 1] & 0x01) == 0) ? 7 : 9;
                if (size > headerLen) {
                    byte[] raw = new byte[size - headerLen];
                    System.arraycopy(data, offset + headerLen, raw, 0, size - headerLen);
                    return raw;
                }
                return null;
            }
            // 不是ADTS，可能已经是raw
            byte[] raw = new byte[size];
            System.arraycopy(data, offset, raw, 0, size);
            return raw;
        } catch (Exception e) {
            return null;
        }
    }

    // ========== MP4 Box构建工具 ==========

    private byte[] box(String type, byte[] content) {
        byte[] typeBytes = bytes(type);
        int size = 8 + content.length;
        byte[] result = new byte[size];
        result[0] = (byte) ((size >> 24) & 0xFF);
        result[1] = (byte) ((size >> 16) & 0xFF);
        result[2] = (byte) ((size >> 8) & 0xFF);
        result[3] = (byte) (size & 0xFF);
        System.arraycopy(typeBytes, 0, result, 4, 4);
        System.arraycopy(content, 0, result, 8, content.length);
        return result;
    }

    private byte[] bytes(String s) {
        return s.getBytes();
    }

    private byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] a : arrays) total += a.length;
        byte[] result = new byte[total];
        int pos = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, result, pos, a.length);
            pos += a.length;
        }
        return result;
    }

    private byte[] u32(int v) {
        return new byte[]{
                (byte) ((v >> 24) & 0xFF),
                (byte) ((v >> 16) & 0xFF),
                (byte) ((v >> 8) & 0xFF),
                (byte) (v & 0xFF)
        };
    }

    private byte[] u16(int v) {
        return new byte[]{
                (byte) ((v >> 8) & 0xFF),
                (byte) (v & 0xFF)
        };
    }

    private byte[] u64(long v) {
        return new byte[]{
                (byte) ((v >> 56) & 0xFF),
                (byte) ((v >> 48) & 0xFF),
                (byte) ((v >> 40) & 0xFF),
                (byte) ((v >> 32) & 0xFF),
                (byte) ((v >> 24) & 0xFF),
                (byte) ((v >> 16) & 0xFF),
                (byte) ((v >> 8) & 0xFF),
                (byte) (v & 0xFF)
        };
    }

    private byte[] createMvhd(long durationMs, int timescale) {
        // 简化版mvhd
        return concat(
                new byte[]{0, 0, 0, 0}, // version + flags
                u32(0), // creation time
                u32(0), // modification time
                u32(timescale),
                u32((int) (durationMs * timescale / 1000)),
                u32(0x00010000), // rate
                u16(0x0100), // volume
                new byte[10], // reserved
                // matrix (identity)
                new byte[]{0x00, 0x01, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0x00, 0x01, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0x40, 0x00, 0x00, 0x00},
                new byte[24], // pre_defined
                u32(2) // next_track_id
        );
    }

    private byte[] createVideoTrak(List sizes, List times, long chunkOffset,
                                    int width, int height, byte[] avcC, int timescale) {
        ByteArrayOutputStream trak = new ByteArrayOutputStream();
        try {
            // tkhd
            trak.write(box("tkhd", concat(
                    new byte[]{0, 0, 0, 7}, // version + flags (enabled, in_movie, in_preview)
                    u32(0), u32(0),
                    u32(1), // track id
                    u32(0), // reserved
                    u32((int) getTotalDuration(times)), // duration
                    new byte[8], // reserved
                    u16(0), u16(0), // layer, alternate_group
                    u16(0), // volume (video = 0)
                    u16(0), // reserved
                    new byte[]{0x00, 0x01, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0x00, 0x01, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0x40, 0x00, 0x00, 0x00},
                    u16(width << 16 >> 16), u16(0), // width (16.16 fixed point)
                    u16(height << 16 >> 16), u16(0)  // height
            )));

            // mdia
            ByteArrayOutputStream mdia = new ByteArrayOutputStream();
            mdia.write(box("mdhd", concat(
                    new byte[]{0, 0, 0, 0},
                    u32(0), u32(0),
                    u32(timescale),
                    u32((int) getTotalDuration(times)),
                    u16(0x55C4), u16(0) // language (und)
            )));
            mdia.write(box("hdlr", concat(
                    new byte[]{0, 0, 0, 0},
                    new byte[]{0, 0, 0, 0},
                    bytes("vide"),
                    new byte[12],
                    bytes("VideoHandler\0")
            )));

            // minf
            ByteArrayOutputStream minf = new ByteArrayOutputStream();
            minf.write(box("vmhd", concat(
                    new byte[]{0, 0, 0, 1},
                    u16(0), u16(0), u16(0), u16(0)
            )));
            minf.write(box("dinf", box("dref", concat(
                    new byte[]{0, 0, 0, 0},
                    u32(1),
                    box("url ", concat(new byte[]{0, 0, 0, 1}))
            ))));

            // stbl
            ByteArrayOutputStream stbl = new ByteArrayOutputStream();
            stbl.write(box("stsd", concat(
                    new byte[]{0, 0, 0, 0},
                    u32(1),
                    createAvc1(width, height, avcC)
            )));
            stbl.write(box("stts", createStts(times)));
            stbl.write(box("stsc", concat(
                    new byte[]{0, 0, 0, 0},
                    u32(1),
                    u32(1), u32(sizes.size()), u32(0)
            )));
            stbl.write(box("stsz", createStsz(sizes)));
            stbl.write(box("stco", concat(
                    new byte[]{0, 0, 0, 0},
                    u32(1),
                    u32((int) chunkOffset)
            )));

            minf.write(box("stbl", stbl.toByteArray()));
            mdia.write(box("minf", minf.toByteArray()));
            trak.write(box("mdia", mdia.toByteArray()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trak.toByteArray();
    }

    private byte[] createAvc1(int width, int height, byte[] avcC) {
        return box("avc1", concat(
                new byte[6], // reserved
                u16(1), // data_reference_index
                u16(0), u16(0), u16(0), u16(0), // pre_defined, reserved
                u32(0), u32(0), // pre_defined
                u16(width), u16(height),
                u32(0x00480000), // horizresolution (72 dpi)
                u32(0x00480000), // vertresolution
                u32(0), // reserved
                u16(1), // frame_count
                new byte[32], // compressorname
                u16(0x0018), // depth
                u16(0xFFFF), // pre_defined
                avcC != null ? box("avcC", avcC) : new byte[0]
        ));
    }

    private byte[] createAudioTrak(List sizes, List times, long chunkOffset,
                                    int sampleRate, int channels, byte[] esds, int timescale) {
        ByteArrayOutputStream trak = new ByteArrayOutputStream();
        try {
            trak.write(box("tkhd", concat(
                    new byte[]{0, 0, 0, 7},
                    u32(0), u32(0),
                    u32(2), // track id
                    u32(0),
                    u32((int) getTotalDuration(times)),
                    new byte[8],
                    u16(0), u16(0),
                    u16(0x0100), // volume
                    u16(0),
                    new byte[]{0x00, 0x01, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0x00, 0x01, 0x00, 0x00, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0x40, 0x00, 0x00, 0x00},
                    u16(0), u16(0),
                    u16(0), u16(0)
            )));

            ByteArrayOutputStream mdia = new ByteArrayOutputStream();
            mdia.write(box("mdhd", concat(
                    new byte[]{0, 0, 0, 0},
                    u32(0), u32(0),
                    u32(timescale),
                    u32((int) getTotalDuration(times)),
                    u16(0x55C4), u16(0)
            )));
            mdia.write(box("hdlr", concat(
                    new byte[]{0, 0, 0, 0},
                    new byte[]{0, 0, 0, 0},
                    bytes("soun"),
                    new byte[12],
                    bytes("SoundHandler\0")
            )));

            ByteArrayOutputStream minf = new ByteArrayOutputStream();
            minf.write(box("smhd", concat(
                    new byte[]{0, 0, 0, 0},
                    u16(0), u16(0), u32(0)
            )));
            minf.write(box("dinf", box("dref", concat(
                    new byte[]{0, 0, 0, 0},
                    u32(1),
                    box("url ", concat(new byte[]{0, 0, 0, 1}))
            ))));

            ByteArrayOutputStream stbl = new ByteArrayOutputStream();
            stbl.write(box("stsd", concat(
                    new byte[]{0, 0, 0, 0},
                    u32(1),
                    createMp4a(sampleRate, channels, esds)
            )));
            stbl.write(box("stts", createStts(times)));
            stbl.write(box("stsc", concat(
                    new byte[]{0, 0, 0, 0},
                    u32(1),
                    u32(1), u32(sizes.size()), u32(0)
            )));
            stbl.write(box("stsz", createStsz(sizes)));
            stbl.write(box("stco", concat(
                    new byte[]{0, 0, 0, 0},
                    u32(1),
                    u32((int) chunkOffset)
            )));

            minf.write(box("stbl", stbl.toByteArray()));
            mdia.write(box("minf", minf.toByteArray()));
            trak.write(box("mdia", mdia.toByteArray()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trak.toByteArray();
    }

    private byte[] createMp4a(int sampleRate, int channels, byte[] esds) {
        // 如果没有esds，构造一个简单的AAC配置
        byte[] config = esds;
        if (config == null || config.length == 0) {
            // 简化的AAC ESDS
            config = new byte[]{
                    0x03, 0x19, 0x00, 0x00, 0x00, 0x04, 0x11, 0x40, 0x15, 0x00,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05, 0x02,
                    0x12, 0x10, 0x06, 0x01, 0x02
            };
        }

        return box("mp4a", concat(
                new byte[6],
                u16(1),
                u16(0), u16(0), u32(0), u32(0),
                u16(channels),
                u16(16), // sampleSize
                u16(0), u16(0),
                u32(sampleRate << 16),
                box("esds", config)
        ));
    }

    private byte[] createStts(List times) {
        // 简化：假设所有帧duration相同
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(new byte[]{0, 0, 0, 0});
            if (times.size() > 0) {
                // 计算平均duration
                long total = 0;
                for (int i = 0; i < times.size(); i++) {
                    total += ((Long) times.get(i)).longValue();
                }
                long avg = total / times.size();
                out.write(u32(1)); // entry count
                out.write(u32(times.size())); // sample count
                out.write(u32((int) avg)); // sample delta
            } else {
                out.write(u32(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    private byte[] createStsz(List sizes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(new byte[]{0, 0, 0, 0});
            out.write(u32(0)); // sample_size (0 = variable)
            out.write(u32(sizes.size()));
            for (int i = 0; i < sizes.size(); i++) {
                out.write(u32(((Integer) sizes.get(i)).intValue()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    private long getTotalDuration(List times) {
        long total = 0;
        for (int i = 0; i < times.size(); i++) {
            total += ((Long) times.get(i)).longValue();
        }
        return total;
    }
}
