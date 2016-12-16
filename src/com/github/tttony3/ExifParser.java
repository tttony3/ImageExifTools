package com.github.tttony3;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ExifParser {
	private File file;
	private int app1InfoNum = 0;
	private int gpsInfoNum = 0;
	
	/**
	 * int[0] is count,int[1] is offset
	 */
	private Map<ExifTag, int[]> fieldInfo = new HashMap<ExifTag, int[]>();
	
	/**
	 * int[0] is count,int[1] is offset
	 */
	private Map<ExifTag, int[]> gpsInfo = new HashMap<ExifTag, int[]>();
	private Map<ExifTag, byte[]> fieldByteMessage = new HashMap<ExifTag, byte[]>();
	private Map<ExifTag, byte[]> gpsByteMessage = new HashMap<ExifTag, byte[]>();
	private Map<ExifTag, String> fieldStringMessage = new HashMap<ExifTag, String>();
	public Map<ExifTag, int[]> getFieldInfo() {
		return fieldInfo;
	}

	public Map<ExifTag, int[]> getGpsInfo() {
		return gpsInfo;
	}

	public Map<ExifTag, byte[]> getFieldByteMessage() {
		return fieldByteMessage;
	}

	public Map<ExifTag, String> getFieldStringMessage() {
		return fieldStringMessage;
	}

	public Map<ExifTag, byte[]> getGpsByteMessage() {
		return gpsByteMessage;
	}

	public Map<ExifTag, String> getGpsStringMessage() {
		return gpsStringMessage;
	}

	public boolean isGetExifByte() {
		return isGetExifByte;
	}

	public boolean isGetGpsByte() {
		return isGetGpsByte;
	}

	private Map<ExifTag, String> gpsStringMessage = new HashMap<ExifTag, String>();
	private int header;
	private boolean isGetExifByte = false;
	private boolean isGetGpsByte = false;
	public ExifParser(File file) {
		this.file = file;
	}
	
	public boolean loadImage(){
		if(isExifExist()){
			isGetExifByte=getExifByteInfoMap();
			isGetGpsByte=getGpsByteInfoMap();
			if(isGetGpsByte)
				gpsStringMessage = convertMessageByteToString(gpsByteMessage);
			if(isGetExifByte)
				fieldStringMessage = convertMessageByteToString(fieldByteMessage);
			return true;
		}else
			return false;
		
	}

	private boolean getExifByteInfoMap() {
		if (app1InfoNum != 0) {
			InputStream ios = null;
			try {
				byte[] buffer = new byte[header+10 + app1InfoNum * 12];
				ios = new BufferedInputStream(new FileInputStream(file));
				ios.mark(4096);
				ios.read(buffer, 0, header+10 + app1InfoNum * 12);
				for (int i = 0; i < app1InfoNum; i++) {
					int tag = ((buffer[header+10 + i * 12 + 0] & 0xff) << 8) + (buffer[header+10 + i * 12 + 1] & 0xff);
					int count = ((buffer[header+10 + i * 12 + 4] & 0xff) << 24) + ((buffer[header+10 + i * 12 + 5] & 0xff) << 16)
							+ ((buffer[header+10 + i * 12 + 6] & 0xff) << 8) + (buffer[header+10 + i * 12 + 7] & 0xff);
					if (ExifTag.fromId(tag).getType().size * count < 32) {
						fieldByteMessage.put(ExifTag.fromId(tag),
								new byte[] { (buffer[header+10 + i * 12 + 8]), (buffer[header+10 + i * 12 + 9]),
										(buffer[header+10 + i * 12 + 10]), (buffer[header+10 + i * 12 + 11]) });
					} else {
						int offset = ((buffer[header+10 + i * 12 + 8] & 0xff) << 24)
								+ ((buffer[header+10 + i * 12 + 9] & 0xff) << 16)
								+ ((buffer[header+10 + i * 12 + 10] & 0xff) << 8) + (buffer[header+10 + i * 12 + 11] & 0xff);
						int infos[] = { count, offset };
						fieldInfo.put(ExifTag.fromId(tag), infos);
					}
				}
				ios.reset();
				for (ExifTag tag : fieldInfo.keySet()) {
					if ( tag.getId() == 0x8769 || tag.getId() == 0xa005 ||tag.getId() == 0x8825)
						continue;
					byte[] tmpBuffer = new byte[fieldInfo.get(tag)[0] * tag.getType().size / 8];
					ios.skip(fieldInfo.get(tag)[1] + 0xc);
					ios.read(tmpBuffer);
					fieldByteMessage.put(tag, tmpBuffer);
					ios.reset();

				}

				ios.close();
				return true;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			return false;
		}
		return false;
	}
	public boolean getGpsByteInfoMap(){
		if(fieldInfo.get(ExifTag.Image_GPSTag) == null)
			return false;
		int offset =  fieldInfo.get(ExifTag.Image_GPSTag)[1];
		InputStream ios = null;
		byte[] gpsNumBuffer = new byte[2];
		try {
				ios = new BufferedInputStream(new FileInputStream(file));
				ios.mark(4096);
				ios.skip(header+offset);
				ios.read(gpsNumBuffer);
				gpsInfoNum = ((gpsNumBuffer[0]&0xff)<<8)+(gpsNumBuffer[1]&0xff);
				byte[] gpsTagBuffer = new byte[gpsInfoNum*12];
				ios.read(gpsTagBuffer);
				for (int i = 0; i < gpsInfoNum; i++) {
					int tag = ((gpsTagBuffer[ i * 12 + 0] & 0xff) << 8) + (gpsTagBuffer[ i * 12 + 1] & 0xff);
					int count = ((gpsTagBuffer[ i * 12 + 4] & 0xff) << 24) + ((gpsTagBuffer[ i * 12 + 5] & 0xff) << 16)
							+ ((gpsTagBuffer[ i * 12 + 6] & 0xff) << 8) + (gpsTagBuffer[ i * 12 + 7] & 0xff);
					if (ExifTag.fromId(tag).getType().size * count < 32) {
						gpsByteMessage.put(ExifTag.fromId(tag),
								new byte[] { (gpsTagBuffer[ i * 12 + 8]), (gpsTagBuffer[ i * 12 + 9]),
										(gpsTagBuffer[ i * 12 + 10]), (gpsTagBuffer[ i * 12 + 11]) });
					} else {
						offset = ((gpsTagBuffer[ i * 12 + 8] & 0xff) << 24)
								+ ((gpsTagBuffer[ i * 12 + 9] & 0xff) << 16)
								+ ((gpsTagBuffer[ i * 12 + 10] & 0xff) << 8) + (gpsTagBuffer[ i * 12 + 11] & 0xff);
						int infos[] = { count, offset };
						
						gpsInfo.put(ExifTag.fromId(tag), infos);
					}
				}
				
				
				for (ExifTag tag : gpsInfo.keySet()) {
					ios.reset();
					byte[] tmpBuffer = new byte[gpsInfo.get(tag)[0] * tag.getType().size / 8];
					ios.skip(gpsInfo.get(tag)[1] + header);
					ios.read(tmpBuffer);
					gpsByteMessage.put(tag, tmpBuffer);

				}
				ios.close();
			} catch (IOException e) {
				
				e.printStackTrace();
			}	
		return true;
	}

	private Map<ExifTag, String> convertMessageByteToString(Map<ExifTag, byte[]> map) {
		Map<ExifTag, String> stringMap = new HashMap<>();
		for (Entry<ExifTag, byte[]> tmp : map.entrySet()) {
			String message="";
			byte[] bs = tmp.getValue();
			switch (tmp.getKey().getType().id) {
			
			case 1:

				break;
			case 2:
				for(int i =0;i<tmp.getValue().length;i++){
					if(i==tmp.getValue().length-1 && (tmp.getValue()[i]&0xff) == 0x00)
						continue;
					message += (char)(tmp.getValue()[i]&0xff);
					}
				break;
			case 3:
				
				int unit;
				switch (tmp.getKey()) {
				case Image_ResolutionUnit:
					unit= ((bs[0]&0xff)<<8)+((bs[1]&0xff));
					if(unit ==2)
						message += "inches";
					else if(unit ==3)
						message += "centimeters";
					else
						message += "reserved";
					break;
				case Image_YCbCrPositioning:
					unit = ((bs[0]&0xff)<<8)+((bs[1]&0xff));
					if(unit ==1)
						message += "centered";
					else if(unit ==2)
						message += "co-sited";
					else
						message += "reserved";
					break;
				default:
					break;
				}
				break;
			case 4:

				break;
			case 5:
				int num =bs.length/8; 
				for (int i=0;i<num;i++){
					int numerator = ((bs[i*8]&0xff)<<24)+((bs[i*8+1]&0xff)<<16)+((bs[i*8+2]&0xff)<<8)+(bs[i*8+3]&0xff);
					int denominator = ((bs[i*8+4]&0xff)<<24)+((bs[i*8+5]&0xff)<<16)+((bs[i*8+6]&0xff)<<8)+(bs[i*8+7]&0xff);
					if(message.equals("")){
						message += numerator/denominator;
					}else{
						if((tmp.getKey()==ExifTag.GPSInfo_GPSLatitude ||tmp.getKey()==ExifTag.GPSInfo_GPSLongitude )&& i ==2)
							message += ";"+(double)numerator/(double)denominator;
						else
							message += ";"+numerator/denominator;
					}
					
				}
				break;
			case 7:

				break;
			case 9:

				break;
			case 10:

				break;

			default:
				break;
			}
			stringMap.put(tmp.getKey(), message);
		}
		return stringMap;
	}
	
	
	
	
	

	public boolean isExifExist() {
		if (file != null) {
			return isExifExist(file);
		} 
		else
			return false;
	}

	private boolean isExifExist(File file) {
		InputStream ios = null;
		try {
			byte[] bufferTitle = new byte[1024];
			ios = new FileInputStream(file);
			ios.read(bufferTitle, 0, 1024);
			if ((bufferTitle[0x00] & 0xff) == 0xff && (bufferTitle[0x01] & 0xff) == 0xd8)
				for(int i=2;i<1023;i++){
					if((bufferTitle[i] & 0xff) == 0x4d && (bufferTitle[i+1] & 0xff) == 0x4d ){
						header = i;
						app1InfoNum = ((bufferTitle[header+8]&0xff)<<8) + (bufferTitle[header+9]&0xff);
						ios.close();
						return true;
					}
				}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param latitudeA 纬度时
	 * @param latitudeB 纬度分
	 * @param latitudeC 纬度秒
	 * @param longitudeA 经度时
	 * @param longitudeB 经度分
	 * @param longitudeC 经度秒
	 * @param path 生成文件路径
	 * @return
	 */
	public boolean modifyGps(int latitudeA,int latitudeB,int latitudeC,int longitudeA,int longitudeB,int longitudeC,String path){
		if(isGetGpsByte){
			byte[] byteLatitude = gpsByteMessage.get(ExifTag.GPSInfo_GPSLatitude);
			byte[] byteLongitude = gpsByteMessage.get(ExifTag.GPSInfo_GPSLongitude);
			if(byteLatitude!=null && byteLongitude != null){
				byteLatitude[0] =  (byte) 0;
				byteLatitude[1] =  (byte) 0;
				byteLatitude[2] =  (byte) 0;
				byteLatitude[3] =  (byte) latitudeA;
				byteLatitude[4] =  (byte) 0;
				byteLatitude[5] =  (byte) 0;
				byteLatitude[6] =  (byte) 0;
				byteLatitude[7] =  (byte) 1;
				byteLatitude[8] =  (byte) 0;
				byteLatitude[9] =  (byte) 0;
				byteLatitude[10] =  (byte) 0;
				byteLatitude[11] =  (byte) latitudeB;
				byteLatitude[12] =  (byte) 0;
				byteLatitude[13] =  (byte) 0;
				byteLatitude[14] =  (byte) 0;
				byteLatitude[15] =  (byte) 1;
				byteLatitude[16] =  (byte) 0;
				byteLatitude[17] =  (byte) 0;
				byteLatitude[18] =  (byte) 0;
				byteLatitude[19] =  (byte) latitudeC;
				byteLatitude[20] =  (byte) 0;
				byteLatitude[21] =  (byte) 0;
				byteLatitude[22] =  (byte) 0;
				byteLatitude[23] =  (byte) 1;
				
				byteLongitude[0] =  (byte) 0;
				byteLongitude[1] =  (byte) 0;
				byteLongitude[2] =  (byte) 0;
				byteLongitude[3] =  (byte) longitudeA;
				byteLongitude[4] =  (byte) 0;
				byteLongitude[5] =  (byte) 0;
				byteLongitude[6] =  (byte) 0;
				byteLongitude[7] =  (byte) 1;
				byteLongitude[8] =  (byte) 0;
				byteLongitude[9] =  (byte) 0;
				byteLongitude[10] =  (byte) 0;
				byteLongitude[11] =  (byte) longitudeB;
				byteLongitude[12] =  (byte) 0;
				byteLongitude[13] =  (byte) 0;
				byteLongitude[14] =  (byte) 0;
				byteLongitude[15] =  (byte) 1;
				byteLongitude[16] =  (byte) 0;
				byteLongitude[17] =  (byte) 0;
				byteLongitude[18] =  (byte) 0;
				byteLongitude[19] =  (byte) longitudeC;
				byteLongitude[20] =  (byte) 0;
				byteLongitude[21] =  (byte) 0;
				byteLongitude[22] =  (byte) 0;
				byteLongitude[23] =  (byte) 1;
				
				InputStream ios = null;
				OutputStream oos = null;
				try {
					ios = new BufferedInputStream(new FileInputStream(file));
					oos = new BufferedOutputStream(new FileOutputStream("temp0.jpg"));
					
					int latitudeOffset = getGpsInfo().get(ExifTag.GPSInfo_GPSLatitude)[1];
				
					int i = 0;
					//�����СΪ512�ֽ�
					int j = 0;
					byte[] buffer = new byte[512];
					while(true) {
						if(ios.available() < 512) {
							while(i != -1) {
								i = ios.read();
								oos.write(i);
							}
							break;//ע��˴���������Ŷ
						} else {
							//���ļ��Ĵ�С����512�ֽ�ʱ
							if((j*512<latitudeOffset+header)&& ((j+1)*512>latitudeOffset+header+24)){
								int of1 = (latitudeOffset+header)%512;
								int of2 = 512-of1-24;
								byte[] bof1 = new byte[of1];
								byte[] bof2 = new byte[of2];
								ios.read(bof1);
								oos.write(bof1);
								oos.write(byteLatitude);
								ios.skip(24);
								ios.read(bof2);
								oos.write(bof2);
							}
							else if((j*512<latitudeOffset+header)&& ((j+1)*512<latitudeOffset+header+24) && ((j+1)*512>latitudeOffset+header)){
								int of1 = (latitudeOffset+header)%512;
								byte[] bof1 = new byte[of1];
								ios.read(bof1);
								oos.write(bof1);
								oos.write(byteLatitude);
								ios.skip(24);
							}
							else{
							ios.read(buffer);
							oos.write(buffer);
							}
							
							j++;
						}
					}
					ios.close();
					oos.close();
					
				} catch (FileNotFoundException e) {		
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				try {
					ios = new BufferedInputStream(new FileInputStream("temp0.jpg"));
					oos = new BufferedOutputStream(new FileOutputStream(path));
				
					int longitudeOffset = getGpsInfo().get(ExifTag.GPSInfo_GPSLongitude)[1];
					int i = 0;
					//�����СΪ512�ֽ�
					int j = 0;
					byte[] buffer = new byte[512];
					while(true) {
						if(ios.available() < 512) {
							while(i != -1) {
								i = ios.read();
								oos.write(i);
							}
							break;//ע��˴���������Ŷ
						} else {
							//���ļ��Ĵ�С����512�ֽ�ʱ
							if((j*512<longitudeOffset+header)&& ((j+1)*512>longitudeOffset+header+24)){
								int of1 = (longitudeOffset+header)%512;
								int of2 = 512-of1-24;
								byte[] bof1 = new byte[of1];
								byte[] bof2 = new byte[of2];
								ios.read(bof1);
								oos.write(bof1);
								oos.write(byteLatitude);
								ios.skip(24);
								ios.read(bof2);
								oos.write(bof2);
							}else if((j*512<longitudeOffset+header)&& ((j+1)*512<longitudeOffset+header+24) && ((j+1)*512>longitudeOffset+header)){
								int of1 = (longitudeOffset+header)%512;
								byte[] bof1 = new byte[of1];
								ios.read(bof1);
								oos.write(bof1);
								oos.write(byteLatitude);
								ios.skip(24);
							}else{
							ios.read(buffer);
							oos.write(buffer);
							}
							
							j++;
						}
					}
					ios.close();
					oos.close();
					
				} catch (FileNotFoundException e) {		
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				File file = new File("temp0.jpg");
				if(file.exists()){
					file.delete();
				}
				return true;
			}else
				return false;
		}
		return false;
	}
	
}
