-- Initialize the database with a couple of Team emblems to start
-- Chelsea
INSERT INTO Artwork(id, created, file_name, file_path, file_size, height, width, media_type, modified)
VALUES (1, CURRENT_TIMESTAMP, 'chl.png', 'image/emblem/team/', 177805, 400, 400, 'PNG', CURRENT_TIMESTAMP);
UPDATE Teams SET emblem_id=1 WHERE TEAM_ID='8056df0882080a7c1d36f190f231f919';

-- Barcelona
INSERT INTO Artwork(id, created, file_name, file_path, file_size, height, width, media_type, modified)
VALUES (2, CURRENT_TIMESTAMP, 'fcb.png', 'image/emblem/team/', 64731, 400, 400, 'PNG', CURRENT_TIMESTAMP);

-- Liverpool
INSERT INTO Artwork(id, created, file_name, file_path, file_size, height, width, media_type, modified)
VALUES (3, CURRENT_TIMESTAMP, 'liv.png', 'image/emblem/team/', 160077, 400, 400, 'PNG', CURRENT_TIMESTAMP);

-- Manchester City
INSERT INTO Artwork(id, created, file_name, file_path, file_size, height, width, media_type, modified)
VALUES (4, CURRENT_TIMESTAMP, 'mc.png', 'image/emblem/team/', 162638, 400, 400, 'PNG', CURRENT_TIMESTAMP);

