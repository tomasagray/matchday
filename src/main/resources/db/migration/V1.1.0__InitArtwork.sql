-- Create the Artwork table
CREATE TABLE Artwork (
  id INT NOT NULL,
  created TIMESTAMP NOT NULL,
  file_name TEXT NOT NULL,
  file_path LONGTEXT NOT NULL,
  file_size INT,
  height INT,
  width INT,
  media_type LONGTEXT,
  modified TIMESTAMP,
  CONSTRAINT `pk_artwork` PRIMARY KEY (id)
);