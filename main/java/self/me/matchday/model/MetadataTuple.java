/*
 *  All code written by Tomás Gray unless otherwise noted.
 *  May not be reproduced without written consent from the above.
 */
package self.me.matchday.model;

import java.util.Objects;

/** A class representing a key/value pair for a metadata item. */
public class MetadataTuple {
  private final String key;
  private final String value;

  public MetadataTuple(String data, String delimiter) {
    // Split into (hopefully) key/value pairs
    String[] kvPair = data.split(delimiter);

    // Ensure we have a tuple
    if (kvPair.length == 2) {
      this.key = kvPair[0];
      this.value = kvPair[1];
    } else throw new InvalidMetadataException(data);
  }

  /**
   * Returns the tuple key as an uppercase String.
   *
   * @return String The key of the tuple.
   */
  public String getKeyString() {
    return this.key.toUpperCase();
  }

  /**
   * Returns the tuple value
   *
   * @return String The value of the tuple.
   */
  public String getValueString() {
    return this.value;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof MetadataTuple)) return false;

    MetadataTuple kv = (MetadataTuple) o;

    return this.key.equals(kv.key) && this.value.equals(kv.value);
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = (19 * hash) + Objects.hashCode(this.key);
    hash = (19 * hash) + Objects.hashCode(this.value);
    return hash;
  }

  @Override
  public String toString() {
    return "Key: " + this.key + ", Value: " + this.value;
  }
}
