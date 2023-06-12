/*
 * Copyright (c) 2020.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday.plugin.io.ffmpeg;

import java.util.List;
import java.util.Map;
import lombok.Data;

/** Represents audio/video file metadata returned by FFPROBE */
@Data
public class FFmpegMetadata {

  private FFmpegFormat format;
  private List<FFmpegStream> streams;
  private List<FFmpegChapter> chapters;

  @Data
  public static class FFmpegStream {

    private int index;
    private String codec_name;
    private String codec_long_name;
    private String profile;
    private CodecType codec_type;
    private String codec_time_base;
    private String codec_tag_string;
    private String codec_tag;
    private int width, height;
    private int has_b_frames;
    private String sample_aspect_ratio;
    private String display_aspect_ratio;
    private String pix_fmt;
    private int level;
    private String chroma_location;
    private int refs;
    private String is_avc;
    private String nal_length_size;
    private String r_frame_rate;
    private String avg_frame_rate;
    private String time_base;
    private long start_pts;
    private double start_time;
    private long duration_ts;
    private double duration;
    private long bit_rate;
    private long max_bit_rate;
    private int bits_per_raw_sample;
    private int bits_per_sample;
    private long nb_frames;
    private String sample_fmt;
    private int sample_rate;
    private int channels;
    private String channel_layout;
    private FFmpegDisposition disposition;
    private Map<String, String> tags;

    public enum CodecType {
      VIDEO,
      AUDIO
    }

    @Data
    public static class FFmpegDisposition {
      private int _default;
      private int dub;
      private int original;
      private int comment;
      private int lyrics;
      private int karaoke;
      private int forced;
      private int hearing_impaired;
      private int visual_impaired;
      private int clean_effects;
      private int attached_pic;
      private int captions;
      private int descriptions;
      private int metadata;
    }
  }

  @Data
  public static class FFmpegFormat {
    private String filename;
    private int nb_streams;
    private int nb_programs;
    private String format_name;
    private String format_long_name;
    private double start_time;
    private double duration;
    private long size;
    private long bit_rate;
    private int probe_score;
    private Map<String, String> tags;
  }

  @Data
  public static class FFmpegChapter {

    private int id;
    private String time_base;
    private long start;
    private String start_time;
    private long end;
    private String end_time;
  }
}
