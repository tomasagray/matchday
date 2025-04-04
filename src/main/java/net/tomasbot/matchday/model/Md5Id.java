package net.tomasbot.matchday.model;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Data
@NoArgsConstructor
@Embeddable
public class Md5Id implements Serializable {

  private static final MessageDigest MD5;
  private static final HexFormat HEX_FORMAT;

  static {
    try {
      MD5 = MessageDigest.getInstance("md5");
      HEX_FORMAT = HexFormat.of();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private String id;

  public Md5Id(String data) {
    if (data != null) {
      this.id = encode(data);
    }
  }

  public static synchronized String encode(@NotNull String data) {
    byte[] asBytes = data.getBytes(StandardCharsets.UTF_8);
    MD5.update(asBytes);
    byte[] digest = MD5.digest();
    return HEX_FORMAT.formatHex(digest);
  }
}
