package com.realife.l9droid;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class MainActivity extends Activity implements OnClickListener,OnEditorActionListener {
	
	public final static int MACT_L9WORKING = 0;
	public final static int MACT_L9WAITFORCOMMAND = 1;
	public final static int MACT_PRINTCHAR = 2;
	public final static int MACT_SAVEGAMESTATE = 3;
	public final static int MACT_LOADGAMESTATE=4;
	public final static int MACT_GFXON=5;
	public final static int MACT_GFXOFF=6;
	public final static int MACT_GFXUPDATE=7;
	
	Button bCmd;
	EditText etLog;
    EditText etCmd;
    
    Handler h;
    Thread t,g;

    ImageView ivScreen;
    Bitmap bm=null;
    
    String command;
    
    L9implement l9;
    byte gamedata[];
    
    boolean saveload_flag=false;
    static boolean gfx_ready=false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        ivScreen=(ImageView) findViewById(R.id.imageView1);
                       
        bCmd = (Button) findViewById(R.id.bCmd);
        bCmd.setOnClickListener(this);
        
        etLog = (EditText) findViewById(R.id.etLog);
        etCmd = (EditText) findViewById(R.id.etCmd);
        etCmd.setOnEditorActionListener(this);

        etCmd.setText("");
        etLog.setText("Welcome to Level9 emulator v0.001\n(c)2012 Paul Stakhov\n");
                
        command=null;
        
        gamedata=new byte[49179];
        
		h = new Handler() {
		    public void handleMessage(android.os.Message msg) {
		    	switch (msg.what) {
		    	case MACT_L9WORKING:
		    		bCmd.setText("...");
		    		bCmd.setEnabled(false);
		    		break;
		    	case MACT_L9WAITFORCOMMAND:
		    		bCmd.setText("Do");
		    		bCmd.setEnabled(true);
		    		break;
	    		case MACT_PRINTCHAR:
	    			char c=(char)msg.arg1;
	    			if (c==0x0d) etLog.append("\n");
	    			else etLog.append(String.valueOf(c));
	    			break;
	    		case MACT_SAVEGAMESTATE:
    				l9.saveok=fileSave(l9.saveloadBuff);
    				l9.saveloaddone=true;
	    			break;
	    		case MACT_LOADGAMESTATE:
	    			l9.saveloadBuff=fileLoad();
	    			l9.saveloaddone=true;
	    			break;
	    		case MACT_GFXOFF:
	    			ivScreen.setImageBitmap(null);
	    			break;
	    		case MACT_GFXON:
	    			ivScreen.setImageBitmap(bm);
	    			break;
	    		case MACT_GFXUPDATE:
	    			if (bm==null || bm.getHeight()!=l9.PicHeight || bm.getWidth()!=l9.PicWidth) {
	    				if (l9.PicHeight>0 && l9.PicWidth>0)
	    					bm=Bitmap.createBitmap(l9.PicWidth, l9.PicHeight, Bitmap.Config.ARGB_8888);
	    				else bm=null;
	    				ivScreen.setImageBitmap(bm);
	    			};
	    			if (l9.PicBuff!=null) {
	    				for (int y=0;y<l9.PicHeight;y++)
	    					for (int x=0;x<l9.PicWidth;x++)
	    						bm.setPixel(x, y, l9.SelectedPalette[l9.PicBuff[x+y*l9.PicWidth]]);
	    			};
	    			ivScreen.invalidate();
	    				
	    			break;
		    	}
		    };
		};
		h.sendEmptyMessage(MACT_L9WORKING);
        
		try {
			//InputStream is=getResources().openRawResource(R.raw.timev2);
			InputStream is=getResources().openRawResource(R.raw.wormv3);
			is.read(gamedata);            
		} catch (IOException e) {
			e.printStackTrace();
		}
        
		t = new Thread(new Runnable() {
			public void run() {
		        l9=new L9implement(gamedata,h);
		        if (l9.LoadGame("test", "")==true) {
			        while (l9.L9State!=l9.L9StateStopped) {
			        	if (l9.L9State==l9.L9StateWaitForCommand) {
			        		h.sendEmptyMessage(MACT_L9WAITFORCOMMAND);
			        		//TODO: ��������� try-catch �� �����������, �� ����� �� ��� ��������� � ���, ��� ����������, ���� �������� exception?
							try {
								while (command==null) 
									TimeUnit.MILLISECONDS.sleep(200);
								h.sendEmptyMessage(MACT_L9WORKING);
								//TODO: t.wait - ��������, ����� ���������� �������.
								l9.InputCommand(command);
								command=null;
							} catch (InterruptedException e) {
								e.printStackTrace();
							};
			        	} else l9.step();
			        };
		        }
			}
		});
		t.start();
		
		gfx_ready=false;
		
		g = new Thread(new Runnable() {
			public void run() {
				while(true) {
					try {
						if (gfx_ready) {
							if (l9.L9DoPeriodGfxTask()) {
								h.sendEmptyMessage(MACT_GFXUPDATE);
								TimeUnit.MILLISECONDS.sleep(50);
							}
							else TimeUnit.MILLISECONDS.sleep(500);
						} else TimeUnit.MILLISECONDS.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					};
				}
			}
		});
		g.start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // some text
	//@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bCmd: // ������ ����� �������
			postCommand();
			break;
		}
	}

	public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
		postCommand();
		return true;
	}

	void postCommand() {
		//TODO: ��� ������� - ������� �������, �������� � command - �� ������ � 3�4 �������
		if (etCmd.length()>0 && l9.L9State==l9.L9StateWaitForCommand) {
			etLog.append(etCmd.getText()+"\n");
			command=etCmd.getText().toString();
			etCmd.setText("");
		};
		
		//bm.eraseColor(Color.argb(20, 60, 70, 80));
		ivScreen.setImageBitmap(bm);
        ivScreen.setScaleType(ScaleType.FIT_XY);
        ivScreen.invalidate();
	};
	
	public boolean fileSave(byte buff[]) {
		try {
			OutputStream out = openFileOutput ("1.sav", MODE_PRIVATE);
			out.write(buff);
			out.close();
			return true;
		} catch (FileNotFoundException e) {
			//TODO: e.printStackTrace();
		} catch (IOException e) {
			//TODO: e.printStackTrace();
		}
		return false;
	}
	
	public byte[] fileLoad() {
		try {
			InputStream in=openFileInput("1.sav");
			byte tempbuff[]=new byte[0x2000];
			int len=in.read(tempbuff);
			byte buff[]=new byte[len];
			for (int i=0;i<len;i++) buff[i]=tempbuff[i];
			in.close();
			return buff;
		} catch (FileNotFoundException e) {
			//TODO: e.printStackTrace();
		} catch (IOException e) {
			//TODO: e.printStackTrace();
		}
		return null;
	}

}

class L9implement extends L9 {
    String cmdStr;
    DebugStorage ds;
    String vStr;
    Handler mHandler;
    Message msg;
    byte saveloadBuff[];
    boolean saveloaddone;
    boolean saveok;
	
	EditText et;
	byte gamedata[];
	
	//Gfx
	byte PicBuff[]=null;
	int PicWidth=0;
	int PicHeight=0;
	int PicMode;
	int iPicturesSpeed=10;
	
	int iPicturesPalette=0;
	
	int PaletteAmiga[]={
			0xff000000,
			0xffff0000,
			0xff30e830,
			0xffffff00,
			0xff0000ff,
			0xffa06800,
			0xff00ffff,
			0xffffffff
	};

	//these colors taken from unreal speccy emulator, default palette
	//on speccy 8 colors plus 2 grade of bright.
	//in l9 games bright often set to 0 on pictures.

	int PaletteSpectrum[]={
			0xff000000,		//(black)  0
			0xffC00000,		//(red)    2
			0xff00C000,		//(green)  4
			0xffC0C000,		//(yellow) 6
			0xff0000C0,		//(blue)   1
			0xffC000C0,		//(brown)  3
			0xff00C0C0,		//(cyan)   5
			0xffC0C0C0		//(white)  7
	};

	int L9PaletteIndexes[]={0,0,0,0};
	int SelectedPalette[];

	
	//������ ������ �������, � ������.
	final static int L9Fill_StackSize=512;
	int L9_FillStack[];
	//������� ���������� ������ �������
	int L9_FillCount;
	//������� ����� ��� �������.
	int L9_FillColour1;
	int L9_FillColour2;
	
	L9implement(/*EditText et1,*/ byte dat[], Handler h) {
		//et=et1;
		gamedata=dat;
		cmdStr=null;
		ds=new DebugStorage();
		mHandler=h;
		
		L9_FillStack=new int[L9Fill_StackSize];
		SelectedPalette=new int[32];
	};
	
	void os_printchar(char c) {
		if (c==0x0d) log_debug(ds.getstr());
		else if (ds.putchar(c)) log_debug(ds.getstr());
		msg = mHandler.obtainMessage(MainActivity.MACT_PRINTCHAR, c, 0);
		mHandler.sendMessage(msg);
	};
	
	byte[] os_load(String filename) {
		return gamedata;
	};
	
	void os_debug(String str) {
		log_debug(ds.getstr());
		log_debug(str);
	};
	
	void os_verbose(String str) {
		log_verbose(str);
	};
	
	void log_debug(String str) {
		final String LOG_TAG = "l9droid";
		if (str.length()>0) 
			Log.d(LOG_TAG, str);
	};
	
	void log_verbose(String str) {
		final String LOG_TAG = "l9droid";
		if (str.length()>0) 
			Log.v(LOG_TAG, str);
	};
	
	void os_flush() {
		os_debug(ds.getstr());
	}
	
	void step() {
		while (L9State==L9StateRunning || L9State==L9StateCommandReady) RunGame();
	};
	
	boolean os_save_file(byte[] buff) {
		saveloadBuff=buff;
		saveloaddone=false;
		saveok=false;
		mHandler.sendEmptyMessage(MainActivity.MACT_SAVEGAMESTATE);
		while (saveloaddone==false) {
			try {
				TimeUnit.MILLISECONDS.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			};
		};
		return saveok;
	};
	
	byte[] os_load_file() {
		saveloadBuff=null;
		saveloaddone=false;
		mHandler.sendEmptyMessage(MainActivity.MACT_LOADGAMESTATE);
		while (saveloaddone==false) {
			try {
				TimeUnit.MILLISECONDS.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			};
		};
		return saveloadBuff; //TODO:mAct.fileLoad();
	};

	void os_graphics(int mode) {
		int pw[]={0};
		int ph[]={0};
		if (mode==2) {
			/*TODO:L9BitmapType = DetectBitmaps(L9Dir);
			if (L9BitmapType==NO_BITMAPS)*/ mode=0;
		};
		PicMode = mode;
		if (mode==0) mHandler.sendEmptyMessage(MainActivity.MACT_GFXOFF);
		else mHandler.sendEmptyMessage(MainActivity.MACT_GFXON);
		GetPictureSize(pw,ph);
		PicWidth=pw[0];
		PicHeight=ph[0];
		if (PicWidth<=0 || PicHeight<=0 || mode==0) return;
		PicBuff=new byte[PicWidth*PicHeight];
		MainActivity.gfx_ready=true;
		
	};
	
	void os_cleargraphics() {
		if (PicMode==0 || PicMode==2 || PicBuff==null /*|| iApV->iPicturesEnabled==EFalse*/) return;
		//memclr(PicBuff,PicHeight*PicWidth);
		for (int i=0;i<PicHeight*PicWidth;i++) PicBuff[i]=0;
		L9_FillCount=0; //�������� ��������, ���� ��� �����������.
		//TODO:iApV->DrawDeferred();
		
	};

	void L9UpdatePalette() {
		for (int i=0;i<4;i++) {
			if (iPicturesPalette==0)
				 SelectedPalette[i]=PaletteSpectrum[L9PaletteIndexes[i]];
			else SelectedPalette[i]=PaletteAmiga[L9PaletteIndexes[i]];
		};
	};
	
	void os_setcolour(int colour, int index) {
		if (PicMode==0 || PicMode==2) return;
		if ((index>7) || (index<0) || (colour >3) || (colour<0)) return;
		L9PaletteIndexes[colour]=index;
		L9UpdatePalette();
		//TODO:iApV->DrawDeferred();
	};
	
	void L9Plot(int x, int y, int colour1, int colour2) {
		if (x<0 || x>=PicWidth || y<0 || y>=PicHeight) return; 
		if (PicBuff[y*PicWidth+x]==colour2) PicBuff[y*PicWidth+x]=(byte)colour1; 
	};

	//returns 255 if not applicable
	byte L9Point(int x, int y) {
		if (x<0 || x>=PicWidth || y<0 || y>=PicHeight) return (byte)0xff;
		return PicBuff[y*PicWidth+x];
	};

	
	/*
	 * �������� ���������� ��� ��������� �����
	 * function line(x0, x1, y0, y1)
	     int deltax := abs(x1 - x0)
	     int deltay := abs(y1 - y0)
	     int error := 0
	     int y := y0
	     for x from x0 to x1
	         plot(x,y)
	         error := error + deltay
	         if 2 * error >= deltax
	             y := y + 1
	             error := error - deltax
	 * 
	 */
	
	//
	//y=k*x+b
	//k=(y2-y1)/(x2-x1)
	//b=y-k*x
	//

	void os_drawline(int x1, int y1, int x2, int y2, int colour1, int colour2)
	{
		if (PicMode==0 || PicMode==2) return;

		int x0;
		int y0;
		int sx = Math.abs(x2-x1);
		int sy = Math.abs(y2-y1);
		int zx = ((x2>x1)?1:(-1));
		int zy = ((y2>y1)?1:(-1));
		int err=0;
		
		if (sx>=sy) {
			y0=y1;
			for (x0=x1; x0!=x2; x0+=zx) {
				L9Plot(x0, y0, colour1, colour2);
				err+=sy;
				if (2*err >= sx) {
					y0+=zy;
					err-=sx;
				};
			};
			//����������� ����� � �������������� ���������� �������.
			L9Plot(x2, y2, colour1, colour2);
		} else if (sy>sx) {
			x0=x1;
			for (y0=y1; y0!=y2; y0+=zy) {
				L9Plot(x0, y0, colour1, colour2);
				err+=sx;
				if (2*err >= sy) {
					x0+=zx;
					err-=sy;
				};
			};
			//����������� ����� � �������������� ���������� �������.
			L9Plot(x2, y2, colour1, colour2);
		};
	};
	
	void os_fill(int x, int y, int colour1, int colour2) {
		L9Fill_Start (x, y, colour1, colour2);
	};
	
	void L9Fill_Start (int x, int y, int colour1, int colour2) {
		L9_FillCount=0;
		L9_FillColour1=colour1;
		L9_FillColour2=colour2;
		if (PicMode==0 || PicMode==2) return;
		if (x<0 || x>PicWidth || y<0 || y> PicHeight) return; 
		if (L9_FillCount<L9Fill_StackSize-2) {
			L9_FillStack[L9_FillCount++]=x;
			L9_FillStack[L9_FillCount++]=y;
		};
	};

	int L9Fill_Step() {
		int x;
		int y;
		if (L9_FillCount>0) {
			y=L9_FillStack[--L9_FillCount];
			x=L9_FillStack[--L9_FillCount];
			
			//if (L9Point(x,y) != colour2) return;
			//������ ������ ������������� ����� � ���� �����
			boolean FillingUp   = false; //������� ����, ��� ���������� ����� ��� ��������
			boolean FillingDown = false;
			while ((x>0)        && (L9Point(x-1,y)==L9_FillColour2)) x--;
			//��� ������ ����� ����� - ������������, � �������� ������� ������ � ����� �� ���������������.
			while ((x<PicWidth) && (L9Point(x,y)==L9_FillColour2)) {
				L9Plot(x, y, L9_FillColour1, L9_FillColour2);
				if (y-1>=0) {
					if (L9Point(x,y-1)==L9_FillColour2) {
						if (!FillingUp) {
							//os_fill(x,y-1,colour1,colour2);
							if (L9_FillCount<L9Fill_StackSize-2) {
								L9_FillStack[L9_FillCount++]=x;
								L9_FillStack[L9_FillCount++]=y-1;
							};
							FillingUp = true;
						};
					} else FillingUp=false;
				};
				if (y+1<PicHeight) {
					if (L9Point(x,y+1)==L9_FillColour2) {
						if (!FillingDown) {
							//os_fill(x,y+1,colour1,colour2);
							if (L9_FillCount<L9Fill_StackSize-2) {
								L9_FillStack[L9_FillCount++]=x;
								L9_FillStack[L9_FillCount++]=y+1;
							};
							FillingDown = true;
						};
					} else FillingDown=false;
				};
				x++;
			};
		};
		return L9_FillCount;
	};
	
	boolean L9DoPeriodGfxTask() {
		if (PicMode==0 || PicMode==2 /*|| iApV->iPicturesEnabled==EFalse*/) return false;
		
		//�������� ���������� Fill. 
		int j=0;
		for (int i=0; i<iPicturesSpeed; i++)
			if (L9Fill_Step()>0) j++;
			else if (RunGraphics()) j++;	//���� ���������� fill - ������ ��������� ������ ��������
		return j!=0;
	};


}

class DebugStorage {
    private char[] debug;
    private int debugptr;
    private static final int debugsize=500;
    DebugStorage() {
		debug=new char[debugsize];
		debugptr=0;
    }
    boolean putchar(char c) {
    	debug[debugptr++]=c;
    	return (debugptr>=debugsize);
    }
    String getstr() {
    	String str=String.valueOf(debug, 0, debugptr);
    	debugptr=0;
    	return str;
    }
}
