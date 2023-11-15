ALTER TABLE ${prefix}ContentFile
    CHANGE COLUMN file_name file_name VARCHAR(256) NOT NULL;

ALTER TABLE ${prefix}ContentFolder
    CHANGE COLUMN folder_name folder_name VARCHAR(256) NOT NULL;