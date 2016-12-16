# ImageExifTools
#Usage
    ExifParser exifParser = new ExifParser(new File("YOUR FILE"));
    exifParser.loadImage();
    exifParser.getFieldStringMessage();//get field messages ,return map
    exifParser.getGpsStringMessage();//get gps messages ,return map
    exifParser.modifyGps(1,1,1,1,1,1,"modify_img.jpg"); //modify gps info ,input  latitude and longitude and path to save the img,for example intput(1,1,1,1,1,"modify_img.jpg") means latitude is 1:1:1 and latitude is 1:1:1 
