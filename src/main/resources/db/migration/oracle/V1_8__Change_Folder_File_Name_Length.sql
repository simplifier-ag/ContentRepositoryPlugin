ALTER TABLE ${prefix}CONTENTFILE
    MODIFY (FILE_NAME VARCHAR2(256 BYTE) );

ALTER TABLE ${prefix}CONTENTFOLDER
    MODIFY (FOLDER_NAME VARCHAR2(256 BYTE) );