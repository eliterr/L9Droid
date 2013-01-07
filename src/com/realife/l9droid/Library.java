package com.realife.l9droid;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.realife.l9droid.LibraryGameDownloadActivity.DownloadInstallFileTask;

public class Library {

	final String LIBDIR_SD = "/L9Droid/";
	final String DIR_SD = "Worm In Paradise/Speccy";
	final String FILE_SD="worm.sna";
	
	Handler h;
	String GameFullPathName;
	String paths[];

	Library() {
		h=null;
		paths=null;
		GameFullPathName="";
	};
	
	String tags[][]={
			{"Amiga","Amiga"},
			{"Atari","Atari?"},
			{"BBC","BBC"},
			{"CPC","CPC?"},
			{"C64","Commodore 64"},
			{"S48","Speccy 48k"},
			{"S128","Speccy 128k"},
			{"ST","ST?"},
			{"PC","PC"},
	};
	
	boolean prepareLibrary(Activity act) {
		paths=null;
		//getting sdcard path
		String sdState = android.os.Environment.getExternalStorageState();
		if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) {
			File sdPath = android.os.Environment.getExternalStorageDirectory();
			sdPath = new File(sdPath.getAbsolutePath() + LIBDIR_SD+DIR_SD);
			if (!sdPath.isDirectory()) {
				Toast.makeText(act, "Creating library", Toast.LENGTH_LONG).show();
				//
				sdPath.mkdirs();
				File sdFile = new File(sdPath, FILE_SD);
			    try {
			    	
			        byte buff[]=new byte[49179];	        
					InputStream is=act.getResources().openRawResource(R.raw.wormv3);
					is.read(buff);            
			    	OutputStream out = new FileOutputStream(sdFile);
	                out.write(buff, 0, buff.length);
	                out.close();
			    } catch (IOException e) {
			      e.printStackTrace();
			      return false; //������ - ���������� � ����������� ����������?
			    }
			};
			requestPaths();
			
		} else return false;
		return true;
	}
	
	public void requestPaths() {
		int paths_num=0;
		String[] temppaths=new String[300];
		File sdPath = android.os.Environment.getExternalStorageDirectory();
		sdPath = new File(sdPath.getAbsolutePath() + LIBDIR_SD);
		File[] pathdirs=sdPath.listFiles();
		if (pathdirs!=null) {
			for (int i=0; i<pathdirs.length; i++) {
				File[] files=pathdirs[i].listFiles(new GameFilter() );
				if (files!=null) 
					for (int j=0;j<files.length; j++)
						if (files[j].isFile()) {
							temppaths[paths_num++]=files[j].getAbsolutePath();
						}
			};
		};
		//TODO: temppaths-lame! kill it!
		if (paths_num>0) {
			paths=new String[paths_num];
			for (int i=0; i<paths_num; i++) paths[i]=temppaths[i];
		};
	};
	
    class GameFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
        	String lowCaseName=name.toLowerCase();
        	long size=(new File(dir.getPath()+"/"+name)).length();
        	if (size<13491) return false;
        	return (lowCaseName.endsWith(".sna")
        			|| (  lowCaseName.endsWith(".dat") && !(lowCaseName.endsWith("gamedat2.dat") || lowCaseName.endsWith("gamedat3.dat")))
        			|| lowCaseName.indexOf('.')<0);
        }
    }

	
	String[] getPaths() {
		return paths;
	};
	
	byte[] fileLoadGame(String path) {
		byte resbuff[]=fileLoadToArray(path);
		//if (resbuff!=null) GameFullPathName=path;
		return resbuff;
	}
	
	void setPath(String path) {
		GameFullPathName=path;
	}
	
	byte[] fileLoadRelativeToArray(String relativePath) {
		return fileLoadToArray(getAbsolutePath(relativePath));
	}
	
	byte[] fileLoadToArray(String absolutePath) {
		if (absolutePath==null) return null;
		byte buff[]=null;
		String sdState = android.os.Environment.getExternalStorageState();
		if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) {
			//File sdPath = android.os.Environment.getExternalStorageDirectory();
			//File sdFile = new File(sdPath.getAbsolutePath() + LIBDIR_SD + path);
			File sdFile = new File(absolutePath);
		    try {
		    	int size=(int)sdFile.length();
		    	if (size>0) {
		    		buff = new byte[size];
			    	InputStream in = new FileInputStream(sdFile);
	                int len=in.read(buff);
	                in.close();
		    	}
		    } catch (IOException e) {
		      e.printStackTrace();
		    }

		    if (buff!=null) sendUserMessage("Loaded: "+absolutePath);
		    else sendUserMessage("ERROR load: "+absolutePath);
		    
		};
		return buff;
	};
	
	boolean fileSaveFromArray(String path,byte buff[]) {
		String sdState = android.os.Environment.getExternalStorageState();
		if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) {
			try {
				File sdFile = new File(path);
				//folder exists?
				File sdPath = new File(sdFile.getParent());
				if (!sdPath.isDirectory()) {
					//create folder
					sdPath.mkdirs();
				};
				OutputStream out = new FileOutputStream(sdFile);
				out.write(buff);
				out.close();
				sendUserMessage("Saved: "+path);
				return true;
			} catch (FileNotFoundException e) {
				//TODO: e.printStackTrace();
			} catch (IOException e) {
				//TODO: e.printStackTrace();
			}
			sendUserMessage("ERROR save: "+path);
		};
		return false;
	}
	
	ArrayList<String> fileLoadToStringArray(String absolutePath) {
		if (absolutePath==null) return null;
		ArrayList<String> buff=new ArrayList<String>();

		try {
			File sdFile = new File(absolutePath);
			InputStream in = new FileInputStream(sdFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String str = "";
			while ((str = br.readLine()) != null) {
				buff.add(str);
			};
			br.close();
			
		} catch (FileNotFoundException e) {
			//TODO: e.printStackTrace();
		} catch (IOException e) {
			//TODO: e.printStackTrace();
		}
	    if (buff!=null) sendUserMessage("Loaded strings: "+absolutePath);
	    else sendUserMessage("ERROR load strings: "+absolutePath);
	    return buff;
	};
	
	boolean fileSaveFromStringArray(String absolutePath, ArrayList<String> buff) {
		String sdState = android.os.Environment.getExternalStorageState();
		if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) {
			try {
				
				File sdFile = new File(absolutePath);
				//folder exists?
				File sdPath = new File(sdFile.getParent());
				if (!sdPath.isDirectory()) sdPath.mkdirs();
				OutputStream out = new FileOutputStream(sdFile);
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
				
				for (int i=0;i<buff.size();i++)
					bw.write(buff.get(i)+"\n");
				bw.close();
				
				sendUserMessage("Saved strings: "+absolutePath);
				return true;
			} catch (FileNotFoundException e) {
				//TODO: e.printStackTrace();
			} catch (IOException e) {
				//TODO: e.printStackTrace();
			}
			sendUserMessage("ERROR save strings: "+absolutePath);
		};
		return false;		
	}
	
	boolean pictureSaveFromBitmap(String path, Bitmap bm) {
		if (bm==null) return false;
		String sdState = android.os.Environment.getExternalStorageState();
		if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) {
			try {
				File sdFile = new File(path);
				//folder exists?
				File sdPath = new File(sdFile.getParent());
				if (!sdPath.isDirectory()) {
					//create folder
					sdPath.mkdirs();
				};
				OutputStream out = new FileOutputStream(sdFile);
				bm.compress(Bitmap.CompressFormat.PNG, 100, out);
				out.close();
				sendUserMessage("Saved: "+path);
				return true;
			} catch (FileNotFoundException e) {
				//TODO: e.printStackTrace();
			} catch (IOException e) {
				//TODO: e.printStackTrace();
			}
			sendUserMessage("ERROR save: "+path);
		};
		return false;
	}
	
	Bitmap pictureLoadToBitmap(String path) {
		Bitmap b=BitmapFactory.decodeFile(path);
		if (b!=null) b=b.copy(Bitmap.Config.ARGB_8888,true); //i need mutable bitmap for draw on it
		if (b!=null) sendUserMessage("Loaded: "+path);
		else sendUserMessage("ERROR load: "+path);
		return b;
	}
	
	boolean deleteFile(String path) {
		//TODO: ���������, ��� ���� � ���� ����������, ����� �� ������ ��� � �� �������!
		String sdState = android.os.Environment.getExternalStorageState();
		if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) {
			File sdFile = new File(path);
			if (sdFile.delete()) {
				sendUserMessage("Deleted: "+path);
				return true;
			}
		};
		sendUserMessage("ERROR delete: "+path);
		return false;
	}

	//returns:
	//	if (relativePath='/****') absolute path = relativePath
	//	else if (relativePath='.****') absolute path = gamePath-gameName
	//	else absolute path = gamePath-gameName+relativePath
	String getAbsolutePath(String relativePath) { 
		String absolutePath=null;
		//TODO: �� ������, ���� ���� �� ����������, � ���� GameFullPathName ������
		File sdFile = new File(GameFullPathName);
		absolutePath=sdFile.getParent()+'/';
		if (relativePath!=null) {
			//if relativePath starts with '/', when assume it is absolute path already, don't change it
			if (relativePath.length()>0) {
				if (relativePath.charAt(0)=='/') {
					absolutePath=relativePath;
				} else if (relativePath.charAt(0)=='.') {
					//do nothing
				} else {
					absolutePath+=relativePath;
				};
			}
		};
		return absolutePath;
	}
	
	boolean FileExist(String path) {
		//TODO: �� ������, ���� ���� �� ����������, � ���� GameFullPathName ������
		String checkPath=getAbsolutePath(path);
		File sdFile = new File(checkPath);
		return sdFile.exists();
	};
	
	String changeFileExtension(String path, String newExtension) {
		if ((path==null) || (newExtension==null)) return null;
		int i=path.length();
		if (i==0) return null;
		int j=i;
		while (i>0) {
			if (path.charAt(--i)=='.') {
				j=i+1;
				break;
			}
		};
		return path.substring(0, j-1)+'.'+newExtension;
	};
	
	private void sendUserMessage(String txt) {
		if (h==null) return;
		Message msg=h.obtainMessage(Threads.MACT_TOAST, txt);
		h.sendMessage(msg);
	}
	
	public boolean importFile(String fileName, String folderName) {
		String sdState = android.os.Environment.getExternalStorageState();
		if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) {
			File sdPath = android.os.Environment.getExternalStorageDirectory();
			File newFolder;
			int index=0;
			do {
				newFolder = new File(sdPath.getAbsolutePath() + LIBDIR_SD+folderName);
				//TODO: ������� ��������� �����, ���� ��� ��� ����������
				index++;
			} while (newFolder.exists());
			File source=new File(fileName);
			if (source.isDirectory()) return copy(fileName,newFolder.toString());
			else if (source.isFile()) {
				newFolder.mkdirs();
				copy(fileName,newFolder.toString()+"/"+source.getName());
			};
		};
		return false; 
	};

	public static boolean copy(String from, String to) {
		try {      
			File fFrom = new File(from);
			if (fFrom.isDirectory()) { // ���� ����������, �������� ��� �� ����������
				File f1 = new File(to); //������� �������� ����������
                if (!f1.exists()) f1.mkdirs();
				String[] FilesList = fFrom.list();
				for (int i = 0; i < FilesList.length; i++)
					if (!copy(from + "/" + FilesList[i], to + "/" + FilesList[i]))
						return false; // ���� ��� ����������� ��������� ������
			} else if (fFrom.isFile()) { // ���� ���� ������ �������� ���
			    File fTo = new File(to);
			    InputStream in = new FileInputStream(fFrom);
			    OutputStream out = new FileOutputStream(fTo);
			    byte[] buf = new byte[1024];
			    int len;
			    while ((len = in.read(buf)) > 0) {
			        out.write(buf, 0, len);
			    }
			    in.close();
			    out.close();
			}
		} catch (FileNotFoundException ex) { // ��������� ������
		} catch (IOException e) { // ��������� ������
		}
		return true;
	}
	
	//��������� spans � ���� {}
	private String wrapSpans(SpannableStringBuilder spannedStr) {
		//ForegroundColorSpan a[]=spannedString.getSpans(0, 1, ForegroundColorSpan.class);
		String result=new String();
		SpannableStringBuilder spannedString=new SpannableStringBuilder(" ");
		spannedString.append(spannedStr);//����������� ����������� nextSpanTransition, ������� �� ����� 
		int size=spannedString.length();
		int i=spannedString.getSpans(0, size, ForegroundColorSpan.class).length;
		if (i>0) {
			
			//TODO: ���� ������ ���� ������������ ������� � ������ ����� ��������� � �������� ������ 
			//TODO: ����������������� .replace("{", "{/").replace("}", "}}") ��� ���������� ���� ��������
			int begin=0;
			int beginSpan=-1;
			int endSpan=0;
			beginSpan=spannedString.nextSpanTransition(begin, size-1, ForegroundColorSpan.class);
			begin=1; //���������� ����������� ���� ������
			if (beginSpan>=0) {
				endSpan=spannedString.nextSpanTransition(beginSpan, size-1, ForegroundColorSpan.class);
				result+=spannedString.subSequence(begin, beginSpan).toString()/*.replace("{", "{/").replace("}", "}/")*/
						+"{"
						+spannedString.subSequence(beginSpan, endSpan+1).toString()/*.replace("{", "{/").replace("}", "}/")*/
						+"}";
				begin=endSpan+1;
			};
			if (begin<size) result+=spannedString.subSequence(begin, size-1).toString();
			
		} else result=spannedStr.toString()/*.replace("{", "{/").replace("}", "}/")*/;
		return result;
	}
	
	public String getSpannedString(SpannableStringBuilder ssb) {
		String wrapped=wrapSpans(ssb);
		int begin=wrapped.indexOf('{');
		int end=wrapped.lastIndexOf('}');
		if (begin+1<end && begin>0) return wrapped.substring(begin+1, end);
		else return null;
	}
	
	//���������� spans �� ����� {}
	private SpannableStringBuilder unwrapSpans(String wrappedString) {
		
		int size=wrappedString.length();
		int i=wrappedString.indexOf('{', 0);
		int j=wrappedString.indexOf('}', 0);
		if (i>=0 && j>i) {
			SpannableStringBuilder text = new SpannableStringBuilder(wrappedString.subSequence(0, i).toString()
					+wrappedString.subSequence(i+1, j).toString()
					+wrappedString.subSequence(j+1, size).toString());
	        ForegroundColorSpan style = new ForegroundColorSpan(Color.rgb(0, 0, 255)); 
	        text.setSpan(style, i, j-1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
	        return text; 
		} else return new SpannableStringBuilder(wrappedString);
	}
	
	public boolean SaveLogFromSpannableArrayAdapter(String path, ArrayAdapter<SpannableStringBuilder> adapter) {
		ArrayList<String> log=new ArrayList<String>();
    	if (adapter!=null) 
    		for (int i=0; i<adapter.getCount();i++) {
    			log.add((wrapSpans(adapter.getItem(i))));
    		};
		return fileSaveFromStringArray(path, log);
	}
	
	public ArrayList<SpannableStringBuilder> LoadLogToSpannableArrayList(String path) {
		ArrayList<String> log=fileLoadToStringArray(path);
		ArrayList<SpannableStringBuilder> array=new ArrayList<SpannableStringBuilder>();
		if (log!=null) 
			for (int i=0; i<log.size();i++)
				array.add(unwrapSpans(log.get(i)));
		return array;
	}
	
	//������� ������ ����� � ������� ����������� ������, ��������������� ���� gameName
	public ArrayList<String> getInstalledVersions(String gameName) {
		if (paths==null) requestPaths();
		ArrayList<String> p=new ArrayList<String>();
		if (paths!=null) {
			for (int i=0;i<paths.length;i++) {
				if (paths[i].toLowerCase().contains(gameName.toLowerCase())) {
					p.add(paths[i]);
				};
			};
		};
		return p;
	}
	
	public String getTags(String pathFilename) {
		File f = new File(pathFilename);
		String s=f.getParentFile().getName();
		String r="";
		for (int i=0;i<tags.length;i++) {
			if (s.contains(tags[i][0])) {
				if (r.length()>0) r+="/";
				r+=tags[i][1];
			};
		}
		return r;
	}
	
	//�������� ������ ���, � ��������� ��������� �����, �������� ���� � ���������
	public ArrayList<GameInfo> getGameList(Activity act) {
		ArrayList<GameInfo> rez = new ArrayList<GameInfo>();
		String currentCategory="";

		try {
			XmlPullParser parser = act.getResources().getXml(R.xml.games);

			while (parser.next() != XmlPullParser.END_DOCUMENT) {
				if (parser.getEventType() != XmlPullParser.START_TAG) {
					continue;
			    }
				String name = parser.getName();
			    // Starts by looking for the entry tag
			    if (name.equals("game")) {
					GameInfo gi=new GameInfo();

					for (int i=0;i<parser.getAttributeCount();i++) {
						if (parser.getAttributeName(i).equals("name")) gi.setId(parser.getAttributeValue(i));
					};
					while (parser.next() != XmlPullParser.END_TAG) {
						if (parser.getEventType() != XmlPullParser.START_TAG) {
							continue;
						}
						String n = parser.getName();
						if (n.equals("title"))	gi.setTitle(readTag(parser,"title"));
						else {
							skip(parser);
						}
					}
					gi.setCategory(currentCategory);
					rez.add(gi);
					//break;

			    } else if (name.equals("category")) {
			    	for (int i=0;i<parser.getAttributeCount();i++) {
			    		if (parser.getAttributeName(i).equals("name")) currentCategory=parser.getAttributeValue(i);
			    	};
			    }
			};

		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return rez;
	}
	
	public GameInfo getGameInfo(Activity act, String gameName) {
		GameInfo gi=new GameInfo();
		
		String currentCategory="";
		String currentGame="";
		
		//TODO: ������, ����� ��������� ������� ����������
		gi.setCategory("!category!");
		gi.setId("!id!");
		gi.setTitle("!title!");
		gi.setAbout("!About!");
		gi.setAuthors("!authors!");
		
	    try {
	        XmlPullParser parser = act.getResources().getXml(R.xml.games);

	        while (parser.next() != XmlPullParser.END_DOCUMENT) {
	        	if (parser.getEventType() != XmlPullParser.START_TAG) {
	                continue;
	            }
	        	String name = parser.getName();
	            // Starts by looking for the entry tag
	            if (name.equals("game")) {
	            	for (int i=0;i<parser.getAttributeCount();i++) {
	            		if (parser.getAttributeName(i).equals("name")) currentGame=parser.getAttributeValue(i);
	            	};
	            	if (!currentGame.equalsIgnoreCase(gameName)) continue;
	            	while (parser.next() != XmlPullParser.END_TAG) {
	                    if (parser.getEventType() != XmlPullParser.START_TAG) {
	                        continue;
	                    }
	                    String n = parser.getName();
	                    if (n.equals("title"))			gi.setTitle(readTag(parser,"title"));
	                    else if (n.equals("about"))		gi.setAbout(readTag(parser,"about"));
	                    else if (n.equals("authors"))	gi.setAuthors(readTag(parser,"authors"));
	                    else if (n.equals("path"))		{
	                    	for (int i=0;i<parser.getAttributeCount();i++) {
	                    		String atr=parser.getAttributeName(i);
	    	            		if (atr.equals("tags")) gi.addTags(parser.getAttributeValue(i));
	    	            		else if (atr.equals("files")) gi.addFiles(parser.getAttributeValue(i));
	    	            	};
	                    	gi.addPath(readTag(parser,"path"));
	                    }
	                    else {
	                        skip(parser);
	                    }
	                }
	            	gi.setCategory(currentCategory);
	            	gi.setId(currentGame);
	            	break;
	
	            } else if (name.equals("category")) {
	            	for (int i=0;i<parser.getAttributeCount();i++) {
	            		if (parser.getAttributeName(i).equals("name")) currentCategory=parser.getAttributeValue(i);
	            	};
	            }
	            
	        };

	      } catch (XmlPullParserException e) {
	        e.printStackTrace();
	      } catch (IOException e) {
	        e.printStackTrace();
	      }
	    
		return gi;
	}
	
	//from http://developer.android.com/intl/ru/training/basics/network-ops/xml.html#read
	private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
	    if (parser.getEventType() != XmlPullParser.START_TAG) {
	        throw new IllegalStateException();
	    }
	    int depth = 1;
	    while (depth != 0) {
	        switch (parser.next()) {
	        case XmlPullParser.END_TAG:
	            depth--;
	            break;
	        case XmlPullParser.START_TAG:
	            depth++;
	            break;
	        }
	    }
	}
	
	//from http://developer.android.com/intl/ru/training/basics/network-ops/xml.html#read
	// Processes title tags in the feed.
	private String readTag(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
	    parser.require(XmlPullParser.START_TAG, null, tag);
	    String title = readText(parser);
	    parser.require(XmlPullParser.END_TAG, null, tag);
	    return title;
	}
	
	//from http://developer.android.com/intl/ru/training/basics/network-ops/xml.html#read
	// For the tags title and summary, extracts their text values.
	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
	    String result = "";
	    if (parser.next() == XmlPullParser.TEXT) {
	        result = parser.getText();
	        parser.nextTag();
	    }
	    return result;
	}
	
	
	String unifyFolder(String folder) {
		String uF;
		File f;
		int i=0;
		do {
			uF=(i==0)?folder:String.format("%s %d", folder, i);
			i++;
			f = new File(uF);
		} while (f.isDirectory());
		return uF;
	};
	
	public String checkFileInCache(String src) {
		String dst=null;
		String filename=getFileName(src);
		String folder=src.replace('/', '_').replace(":", "_");
		
		String sdState = android.os.Environment.getExternalStorageState();
		if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) {
			File sdPath = android.os.Environment.getExternalStorageDirectory();
			dst=sdPath.getAbsolutePath() + LIBDIR_SD + "_cache/" + folder + "/" + filename;
			File fdst=new File(dst);
			if (fdst.exists()) return dst; //���� ��� ���� ������ �����, ������� ����
		};
		return null;
	};
	
	public boolean checkPathInLibrary(String folderTo) {
		String sdState = android.os.Environment.getExternalStorageState();
		if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) {
			File sdPath = android.os.Environment.getExternalStorageDirectory();
			File path=new File(sdPath.getAbsolutePath() + LIBDIR_SD + folderTo);
			if (path.isDirectory()) return true;
		};
		return false;
	};
	
	public String downloadFileToCache(String src, DownloadInstallFileTask d) {
		String dst=null;
		
		try {
			String filename=getFileName(src);
			String folder=src.replace('/', '_').replace(":", "_");
			
			String sdState = android.os.Environment.getExternalStorageState();
			if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) {
				File sdPath = android.os.Environment.getExternalStorageDirectory();
				dst=sdPath.getAbsolutePath() + LIBDIR_SD + "_cache/" + folder + "/" + filename;
				
				File fdst=new File(dst);
				File dir=fdst.getParentFile();
				if (!dir.isDirectory()) dir.mkdirs();
				
				if (fdst.exists()) return dst; //���� ��� ���� ������ �����, ������ ������� ����
				
				URL url = new URL(src);
				URLConnection connection = url.openConnection();
				int filelenght=connection.getContentLength();
				InputStream in = connection.getInputStream();
				
				OutputStream out = new FileOutputStream(fdst);
			    byte[] buf = new byte[8192];
			    int len;
			    int downloaded=0;
			    while ((len = in.read(buf)) > 0) {
			        out.write(buf, 0, len);
			        downloaded+=len;
			        if (d!=null) d.doProgressUpdate(downloaded, filelenght);
			    }
			    in.close();
			    out.close();
			    
			};

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return dst;
		
	}
	
	String getFileName(String src) {
		return src.substring(src.lastIndexOf('/')+1);
	};
	
	//����� ������������� ��� �����, ������������ � fileToExtract, ��������� ������� � ���, ��� ��� �����,
	//������������ � ����� ���� ����� �������� � ���� �����, ���� ��� ���� ����������
	public boolean unzipFile(String zipPath, String fileToExtract, String folderTo, DownloadInstallFileTask d) {
		ZipFile z;
		ZipEntry ze;
		String absFolderTo;
		int filesUnzipped=0;
		try {
			
			String sdState = android.os.Environment.getExternalStorageState();
			if (sdState.equals(android.os.Environment.MEDIA_MOUNTED)) {
				File sdPath = android.os.Environment.getExternalStorageDirectory();
				absFolderTo=unifyFolder(sdPath.getAbsolutePath() + LIBDIR_SD + folderTo);
				z=new ZipFile(zipPath);
				File path=new File(absFolderTo);
				if (!path.isDirectory()) path.mkdirs();
				
				Enumeration<? extends ZipEntry> e=z.entries();
				while (e.hasMoreElements())
				{
					ze=e.nextElement();
					String zipPathName=ze.getName().toLowerCase();
					if (zipPathName.startsWith(fileToExtract.toLowerCase())) {

						if (!ze.isDirectory()) {
							File fdst=new File(absFolderTo+"/"+getFileName(zipPathName));
							OutputStream out = new FileOutputStream(fdst);
							InputStream in = z.getInputStream(ze);
							byte[] buf = new byte[1024];
						    int len;
						    int filelenght=(int)ze.getSize();
						    int unzipped=0;
						    while ((len = in.read(buf)) > 0) {
						        out.write(buf, 0, len);
						        if (d!=null) 
						        	d.doProgressUpdate(unzipped, filelenght);
						    }
						    in.close();
						    out.close();
						    filesUnzipped++;
						};
					};
				};
			    z.close();
				
				/*
				ze = z.getEntry(fileToExtract);
				if (ze==null) {
					z.close();
					return false;
				};
				*/
				
			} else return false;
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		if (filesUnzipped>0) return true;
		else return false;
	};
}
