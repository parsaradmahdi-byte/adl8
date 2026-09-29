package com.rahedalat.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import java.util.Arrays;

/**
 * Rah Edalat - interactive case engine prototype.
 * Stage 1 is intentionally a state machine rather than a text/quiz sequence.
 */
public class MainActivity extends Activity {
    GameView game;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        w.setStatusBarColor(Color.rgb(10,14,18));
        w.setNavigationBarColor(Color.rgb(10,14,18));
        game = new GameView(this);
        setContentView(game);
    }

    @Override public void onBackPressed() {
        if (!game.back()) super.onBackPressed();
    }

    public static class GameView extends View {
        // Main navigation states.
        static final int START=0, LIBRARY=1, CASE_INFO=2, CASE_ROOM=3,
                BAG=4, PHONE=5, RECEIPT=6, CCTV=7, EVIDENCE=8,
                REASONING=9, LESSON=10, QUIZ=11, RESULT=12;

        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        Bitmap libraryBg, caseRoom, amirStanding, amirTalking, amirFile;

        int screen = START;
        int dialogue = 0;
        boolean dialogueAsked = false;
        boolean bagOpened = false;
        boolean phoneUnlocked = false;
        boolean receiptSolved = false;
        boolean cctvSolved = false;
        boolean[] evidence = new boolean[3];
        boolean[] receiptPlaced = new boolean[4];
        int[] receiptRotation = new int[4];
        int[] phoneDigits = new int[4];
        int phoneCount = 0;
        int receiptPiece = -1;
        float receiptPX, receiptPY;
        float panX = 430;
        float downX, downY;
        boolean draggingPan = false;
        boolean receiptMoved = false;
        float receiptStartX, receiptStartY;
        long downTime;

        // The panoramic room is intentionally wider than the viewport.
        float roomWorldW = 2430f;
        float roomWorldH = 1250f;

        public GameView(Context c) {
            super(c);
            setFocusable(true);
            libraryBg = load(R.drawable.library_bg);
            caseRoom = load(R.drawable.case_room);
            amirStanding = load(R.drawable.amir_standing);
            amirTalking = load(R.drawable.amir_talking);
            amirFile = load(R.drawable.amir_file);
            tp.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        }

        Bitmap load(int id) { return BitmapFactory.decodeResource(getResources(), id); }

        float sx() { return getWidth()/1080f; }
        float sy() { return getHeight()/1920f; }
        float S() { return Math.min(sx(),sy()); }
        float X(float x) { return x*S() + (getWidth()-1080*S())/2f; }
        float Y(float y) { return y*S() + (getHeight()-1920*S())/2f; }
        float LX(float x) { return (x-(getWidth()-1080*S())/2f)/S(); }
        float LY(float y) { return (y-(getHeight()-1920*S())/2f)/S(); }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            c.drawColor(Color.rgb(8,12,16));
            c.save();
            c.translate((getWidth()-1080*S())/2f,(getHeight()-1920*S())/2f);
            c.scale(S(),S());

            if (screen==START) drawStart(c);
            else if (screen==LIBRARY) drawLibrary(c);
            else if (screen==CASE_INFO) drawCaseInfo(c);
            else if (screen==CASE_ROOM) drawCaseRoom(c);
            else if (screen==BAG) drawBag(c);
            else if (screen==PHONE) drawPhone(c);
            else if (screen==RECEIPT) drawReceipt(c);
            else if (screen==CCTV) drawCctv(c);
            else if (screen==EVIDENCE) drawEvidence(c);
            else if (screen==REASONING) drawReasoning(c);
            else if (screen==LESSON) drawLesson(c);
            else if (screen==QUIZ) drawQuiz(c);
            else drawResult(c);

            c.restore();
        }

        void bg(Canvas c, int color) { p.setColor(color); c.drawRect(0,0,1080,1920,p); }
        void overlay(Canvas c,int color) { p.setColor(color); c.drawRect(0,0,1080,1920,p); }
        void round(Canvas c,float l,float t,float r,float b,float rad,int color){p.setColor(color);c.drawRoundRect(l,t,r,b,rad,rad,p);}
        void stroke(Canvas c,float l,float t,float r,float b,float rad,int color,float sw){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(sw);p.setColor(color);c.drawRoundRect(l,t,r,b,rad,rad,p);p.setStyle(Paint.Style.FILL);}

        void text(Canvas c,String s,float cx,float cy,float size,int color,boolean bold,Layout.Alignment align,float width){
            tp.setTextSize(size); tp.setColor(color); tp.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));
            StaticLayout sl = StaticLayout.Builder.obtain(s,0,s.length(),tp,(int)width)
                    .setAlignment(align).setTextDirection(TextDirectionHeuristics.RTL)
                    .setIncludePad(false).build();
            c.save(); c.translate(cx-width/2f,cy-sl.getHeight()/2f); sl.draw(c); c.restore();
        }

        void title(Canvas c,String s){
            p.setColor(0xe3131d24);c.drawRect(0,0,1080,130,p);
            text(c,s,540,66,38,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,820);
        }

        void topBack(Canvas c){
            round(c,35,25,150,115,28,0xdd1a303d);
            text(c,"‹",93,70,58,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,90);
        }

        void drawStart(Canvas c){
            bg(c,0xff0b1116);
            if(caseRoom!=null){p.setAlpha(55);c.drawBitmap(caseRoom,null,new RectF(0,420,1080,975),p);p.setAlpha(255);}
            overlay(c,0x90071117);
            text(c,"راه عدالت",540,610,82,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,950);
            text(c,"پرونده را ببین، محیط را بررسی کن، مدرک را پیدا کن و بعد تصمیم بگیر.",540,760,32,0xffe9dbc5,true,Layout.Alignment.ALIGN_CENTER,900);
            round(c,285,980,795,1140,30,0xff1b5068);
            text(c,"ورود به کتابخانه پرونده‌ها",540,1060,38,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,470);
            text(c,"نسخه بازطراحی‌شده مرحله اول",540,1450,27,0xffcdbda6,true,Layout.Alignment.ALIGN_CENTER,700);
        }

        void drawLibrary(Canvas c){
            bg(c,0xff11181c);
            if(libraryBg!=null)c.drawBitmap(libraryBg,null,new RectF(0,0,1080,1920),p);
            overlay(c,0x55000000);
            title(c,"کتابخانه پرونده‌ها");
            text(c,"برای شروع، خودِ پوشه پرونده اول را لمس کن.",540,250,30,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,900);
            drawFolder(c,540,620,"پولی که ناپدید شد",true);
            drawFolder(c,300,1080,"پرونده دوم",false);
            drawFolder(c,780,1080,"پرونده سوم",false);
            drawFolder(c,300,1480,"پرونده چهارم",false);
            drawFolder(c,780,1480,"پرونده پنجم",false);
        }

        void drawFolder(Canvas c,float x,float y,String label,boolean active){
            int col=active?0xff72502f:0xff45474a;
            round(c,x-150,y-90,x+150,y+105,22,col);
            round(c,x-112,y-120,x-5,y-78,14,col);
            stroke(c,x-150,y-90,x+150,y+105,22,active?0xffd5a85e:0xff74777a,4);
            text(c,label,x,y-5,30,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,260);
            text(c,active?"قابل بازی":"به‌زودی",x,y+55,23,active?0xffefc777:0xff9a9a9a,true,Layout.Alignment.ALIGN_CENTER,220);
        }

        void drawCaseInfo(Canvas c){
            bg(c,0xff0e151a);
            title(c,"اطلاعات اولیه پرونده");
            topBack(c);
            round(c,60,190,1020,1580,34,0xe71a252c);
            text(c,"پرونده اول",950,270,30,0xffe7bd70,true,Layout.Alignment.ALIGN_OPPOSITE,860);
            text(c,"پولی که ناپدید شد",950,350,50,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,860);
            text(c,"موکل: امیر رضایی",950,430,31,0xffdfd0bb,true,Layout.Alignment.ALIGN_OPPOSITE,860);
            text(c,"امیر می‌گوید سه ماه پیش به یکی از آشناها پول قرض داده و قرار بوده تا پایان ماه بازگردانده شود.",950,610,34,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,840);
            text(c,"تو هنوز نباید درباره نتیجه پرونده قضاوت کنی. ابتدا باید در اتاق رسیدگی، مدارک را خودت پیدا و بررسی کنی.",950,840,31,0xffd6c7b3,true,Layout.Alignment.ALIGN_OPPOSITE,840);
            round(c,260,1370,820,1500,28,0xff1c566d);
            text(c,"ورود به اتاق رسیدگی پرونده",540,1435,34,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,520);
        }

        void drawCaseRoom(Canvas c){
            // World image: panoramic, not forced to phone aspect ratio.
            if(caseRoom!=null){
                RectF dst = new RectF(-panX,220,-panX+roomWorldW,220+roomWorldH);
                c.drawBitmap(caseRoom,null,dst,p);
            } else bg(c,0xff151b20);
            overlay(c,0x12000000);
            // Bottom instruction is outside the Android navigation gesture zone.
            round(c,160,1780,920,1870,24,0xd80b151b);
            text(c,"برای دیدن بخش‌های دیگر اتاق، چپ و راست بکش.",540,1825,27,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,700);

            // Amir remains a real character asset, not regenerated.
            Bitmap person = dialogue < 2 ? amirStanding : amirTalking;
            if(person!=null) drawBitmapFit(c,person,735-panX,860,390,720);

            if(dialogue<3){
                drawDialogue(c);
            } else {
                // The actual bag is the desk object. A subtle glow guides the first interaction.
                if(!bagOpened){
                    pulse(c,750-panX,790,44);
                    text(c,"کیف را بررسی کن",750-panX,720,26,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,250);
                }
                if(bagOpened && !phoneUnlocked){
                    text(c,"کیف باز است؛ گوشی و رسید را بررسی کن.",540,250,30,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,850);
                }
                if(bagOpened && phoneUnlocked && receiptSolved && cctvSolved){
                    round(c,310,1600,770,1710,26,0xff245f4c);
                    text(c,"📁  دفترچه مدارک",540,1655,31,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,430);
                }
            }
        }

        void drawDialogue(Canvas c){
            round(c,70,1290,1010,1710,30,0xe9142027);
            String[] lines={
                    "امیر: یه مشکلی برام پیش اومده... پولی که قرض داده بودم برنگشته.",
                    "امیر: هرچی مدرک دارم داخل این کیفه.",
                    "تو: مدرکی هم داری که بشه دقیق بررسیش کرد؟",
                    "امیر: آره. کیف رو روی میز گذاشتم؛ خودت بررسیش کن."
            };
            String s=lines[Math.min(dialogue,lines.length-1)];
            text(c,s,960,1425,34,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,830);
            round(c,365,1580,715,1670,24,0xff1d5267);
            text(c,dialogue==0?"ادامه گفتگو":dialogue==1?"سؤال از امیر":"ادامه",540,1625,29,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,500);
        }

        void drawBag(Canvas c){
            bg(c,0xff11181c); title(c,"باز کردن کیف"); topBack(c);
            round(c,60,190,1020,1660,30,0xff1b252b);
            text(c,"داخل کیف سه سرنخ قرار دارد. هرکدام را جداگانه بررسی کن.",540,280,30,0xffe7dbc9,true,Layout.Alignment.ALIGN_CENTER,900);
            round(c,390,350,690,455,18,0xffd7c39b);
            text(c,"برچسب روی کیف: ۱۸۰۳",540,402,29,0xff3b2d1d,true,Layout.Alignment.ALIGN_CENTER,270);
            text(c,"این چهار رقم سرنخ باز کردن گوشی است.",540,485,23,0xffc9bda9,true,Layout.Alignment.ALIGN_CENTER,650);
            drawEvidenceObject(c,300,700,"گوشی",phoneUnlocked?"باز شده":"قفل است",0xff263d49);
            drawEvidenceObject(c,780,700,"رسید",receiptSolved?"تکمیل شده":"قطعات پراکنده",0xff5d4731);
            drawEvidenceObject(c,540,1120,"تصویر دوربین",cctvSolved?"بررسی شده":"نیاز به بررسی",0xff273a30);
            if(phoneUnlocked && receiptSolved && cctvSolved){
                round(c,300,1450,780,1560,24,0xff28624d);
                text(c,"برگشت به اتاق و ثبت مدارک",540,1505,29,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,520);
            }
        }

        void drawEvidenceObject(Canvas c,float x,float y,String name,String status,int color){
            round(c,x-170,y-135,x+170,y+135,28,color); stroke(c,x-170,y-135,x+170,y+135,28,0xffcaa35d,3);
            if(name.equals("گوشی")){
                round(c,x-48,y-92,x+48,y+92,20,0xff101417); round(c,x-38,y-70,x+38,y+55,8,0xff1e5265);
            } else if(name.equals("رسید")){
                p.setColor(0xffeee4d1); c.drawRect(x-105,y-92,x+105,y+92,p);
                p.setColor(0xff8d6b42); for(int i=0;i<4;i++)c.drawRect(x-78,y-45+i*30,x+75,y-38+i*30,p);
            } else {
                p.setColor(0xff20272b); c.drawRect(x-110,y-75,x+110,y+75,p);
                p.setColor(0xff7fa3ad); c.drawCircle(x,y,32,p); p.setColor(0xff152028);c.drawCircle(x,y,13,p);
            }
            text(c,name,x,y+190,31,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,300);
            text(c,status,x,y+235,23,0xffd5c4aa,true,Layout.Alignment.ALIGN_CENTER,320);
        }

        void drawPhone(Canvas c){
            bg(c,0xff090e12); title(c,"گوشی پیدا شده"); topBack(c);
            round(c,260,190,820,1510,42,0xff141b20); stroke(c,260,190,820,1510,42,0xff6f7b80,5);
            round(c,315,270,765,540,28,0xff172f39);
            String shown=""; for(int i=0;i<phoneCount;i++)shown += "• ";
            text(c,shown.length()==0?"رمز چهاررقمی را وارد کن":shown,540,405,43,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,400);
            int n=1;
            for(int r=0;r<4;r++)for(int col=0;col<3;col++){
                float x=400+140*col,y=690+150*r;
                if(n<=9) drawKey(c,x,y,String.valueOf(n++));
                else if(r==3&&col==1)drawKey(c,x,y,"0");
                else if(r==3&&col==2)drawKey(c,x,y,"⌫");
            }
            text(c,"رمز از روی سرنخ کیف قابل استنتاج است.",540,1390,25,0xffd9c9b3,true,Layout.Alignment.ALIGN_CENTER,700);
            if(phoneUnlocked){
                round(c,310,560,770,1170,30,0xff102a22);
                text(c,"پیام بانکی",540,650,33,0xff8fe1b5,true,Layout.Alignment.ALIGN_CENTER,400);
                text(c,"واریز ۴۵٬۰۰۰٬۰۰۰ ریال\nگیرنده: م. ک\nتاریخ: ۱۴۰۵/۰۳/۱۸",720,790,31,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,600);
                text(c,"این پیام به عنوان یک یافته ثبت شد.",540,1050,26,0xffcfe4d7,true,Layout.Alignment.ALIGN_CENTER,650);
            }
        }

        void drawReceipt(Canvas c){
            bg(c,0xff17120d); title(c,"بازسازی رسید"); topBack(c);
            text(c,"هر چهار قطعه را به جعبه وسط بکش و با چرخش، لبه‌ها را هم‌راستا کن.",540,190,27,0xfff0dfc5,true,Layout.Alignment.ALIGN_CENTER,930);
            round(c,245,380,835,1150,28,0xff33271c); stroke(c,245,380,835,1150,28,0xffb88d51,4);
            text(c,"محل بازسازی رسید",540,445,29,0xffe4c58d,true,Layout.Alignment.ALIGN_CENTER,450);
            // target quadrants
            for(int r=0;r<2;r++)for(int col=0;col<2;col++){
                stroke(c,305+220*col,525+260*r,515+220*col,765+260*r,10,0xff806c53,2);
            }
            if(receiptPlaced[0]) drawPiece(c,410,645,0,0xfff0e6d2,"رسید",receiptRotation[0]);
            if(receiptPlaced[1]) drawPiece(c,630,645,1,0xffe3d5bb,"شماره",receiptRotation[1]);
            if(receiptPlaced[2]) drawPiece(c,410,905,2,0xfff3e8d4,"مبلغ",receiptRotation[2]);
            if(receiptPlaced[3]) drawPiece(c,630,905,3,0xffdfcfb4,"تاریخ",receiptRotation[3]);
            if(!receiptSolved){
                if(!receiptPlaced[0]) drawPiece(c,120,1330,0,0xfff0e6d2,"رسید",0);
                if(!receiptPlaced[1]) drawPiece(c,350,1480,1,0xffe3d5bb,"شماره",1);
                if(!receiptPlaced[2]) drawPiece(c,650,1320,2,0xfff3e8d4,"مبلغ",2);
                if(!receiptPlaced[3]) drawPiece(c,900,1500,3,0xffdfcfb4,"تاریخ",3);
                if(receiptPiece>=0){
                    drawPiece(c,receiptPX,receiptPY,receiptPiece,0xfff5ead7,"قطعه",receiptRotation[receiptPiece]);
                    text(c,"لمس دوباره قطعه = چرخش",540,1580,24,0xffd8c8b0,true,Layout.Alignment.ALIGN_CENTER,600);
                }
            } else {
                round(c,350,1320,730,1450,25,0xff2d624d);
                text(c,"✓ رسید کامل شد",540,1385,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,500);
            }
        }

        void drawPiece(Canvas c,float x,float y,int id,int color,String label,int rot){
            c.save(); c.rotate(rot*90f,x,y);
            round(c,x-85,y-70,x+85,y+70,10,color);
            text(c,label,x,y,25,0xff443624,true,Layout.Alignment.ALIGN_CENTER,150);
            c.restore();
            if(receiptPiece==id) stroke(c,x-92,y-77,x+92,y+77,10,0xfff2c15e,5);
        }

        void drawCctv(Canvas c){
            bg(c,0xff0b1014); title(c,"بررسی دوربین مداربسته"); topBack(c);
            round(c,70,180,1010,1110,30,0xff1b2328);
            // CCTV monitor
            round(c,110,220,970,910,18,0xff050708);
            p.setColor(0xff23343b); c.drawRect(140,250,940,870,p);
            for(int i=0;i<5;i++){p.setColor(0xff29434a);c.drawRect(150,260+i*120,930,264+i*120,p);}
            text(c,"CAM 04  |  18:20:00",190,300,24,0xffb7c9c6,false,Layout.Alignment.ALIGN_NORMAL,300);
            // silhouettes / meeting moment
            p.setColor(0xff182328); c.drawCircle(470,560,75,p); c.drawCircle(650,570,70,p);
            p.setColor(0xff4d6a72);c.drawRect(420,620,520,800,p);c.drawRect(605,625,695,800,p);
            if(cctvSolved){
                round(c,300,500,780,720,24,0x9b173f35);
                text(c,"لحظه ملاقات پیدا شد",540,580,34,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,420);
                text(c,"امیر یک پاکت را به طرف مقابل تحویل می‌دهد.",540,655,28,0xffd6eee7,true,Layout.Alignment.ALIGN_CENTER,620);
            }
            // timeline
            text(c,"خط زمانی را پیدا کن و لحظه ملاقات را بررسی کن.",540,1000,27,0xffe4d5bd,true,Layout.Alignment.ALIGN_CENTER,850);
            round(c,150,1180,930,1225,18,0xff4a5558);
            p.setColor(cctvSolved?0xff5dc58f:0xffc58b4c); c.drawRect(150,1180,cctvSolved?780:450,1225,p);
            for(int i=0;i<6;i++){p.setColor(0xffd0d0d0);c.drawCircle(180+i*145,1202,7,p);}
            round(c,360,1340,720,1460,24,cctvSolved?0xff2b674f:0xff254c5c);
            text(c,cctvSolved?"مدرک دوربین ثبت شد":"بررسی بازه مشکوک",540,1400,29,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,340);
        }

        void drawEvidence(Canvas c){
            bg(c,0xff10171c); title(c,"دفترچه مدارک"); topBack(c);
            text(c,"فقط مدارکی که واقعاً کشف کرده‌ای در اینجا ثبت می‌شوند.",540,190,28,0xffdfcfb9,true,Layout.Alignment.ALIGN_CENTER,900);
            drawEvidenceRow(c,300,430,"گوشی","پیام بانکی و مشخصات واریز",evidence[0]);
            drawEvidenceRow(c,300,690,"رسید","رسید بازسازی‌شده",evidence[1]);
            drawEvidenceRow(c,300,950,"CCTV","لحظه ملاقات و تحویل پاکت",evidence[2]);
            boolean all=evidence[0]&&evidence[1]&&evidence[2];
            round(c,230,1330,850,1460,25,all?0xff28624d:0xff3c4144);
            text(c,"مدارک را یافتم",540,1395,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,500);
            if(all) text(c,"همه مدارک کشف شده‌اند؛ ادامه پرونده فعال است.",540,1560,27,0xff8ed5b2,true,Layout.Alignment.ALIGN_CENTER,850);
            else text(c,"هنوز یک یا چند مدرک ناقص است.",540,1560,27,0xffdf9f86,true,Layout.Alignment.ALIGN_CENTER,850);
        }

        void drawEvidenceRow(Canvas c,float x,float y,String name,String detail,boolean found){
            round(c,x-210,y-80,x+480,y+80,22,found?0xff1d3a30:0xff20292e);
            text(c,found?"✓":"○",x-165,y,42,found?0xff7fe0a8:0xff7e8588,true,Layout.Alignment.ALIGN_CENTER,80);
            text(c,name,x+40,y-25,31,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,350);
            text(c,detail,x+40,y+25,23,0xffd0c5b4,true,Layout.Alignment.ALIGN_OPPOSITE,350);
        }

        void drawReasoning(Canvas c){
            bg(c,0xff0d1419); title(c,"پیوند دادن مدارک"); topBack(c);
            text(c,"حالا نوبت کشف رابطه بین یافته‌هاست؛ هنوز سؤال تستی در کار نیست.",540,190,28,0xffe5d6c1,true,Layout.Alignment.ALIGN_CENTER,920);
            card(c,70,300,1010,600,"۱. پیام بانکی","نشان می‌دهد انتقال وجهی با مبلغ مشخص انجام شده است.");
            card(c,70,680,1010,980,"۲. رسید","مشخصات معامله و تاریخ را روشن می‌کند.");
            card(c,70,1060,1010,1360,"۳. CCTV","امیر را در حال تحویل پاکت به طرف مقابل نشان می‌دهد.");
            round(c,260,1490,820,1630,25,0xff1d5368);
            text(c,"رابطه مدارک را جمع‌بندی کن",540,1560,31,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,500);
            text(c,"این سه یافته، یک زنجیره واحد از پرداخت، زمان و ملاقات می‌سازند.",540,1735,29,0xffcfe0d4,true,Layout.Alignment.ALIGN_CENTER,900);
        }

        void card(Canvas c,float l,float t,float r,float b,String h,String body){
            round(c,l,t,r,b,25,0xff1b252b); stroke(c,l,t,r,b,25,0xff705f4b,2);
            text(c,h,r-35,t+55,30,0xffe8bf77,true,Layout.Alignment.ALIGN_OPPOSITE,850);
            text(c,body,r-35,t+135,27,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,850);
        }

        void drawLesson(Canvas c){
            bg(c,0xff10171b); title(c,"یادگیری حقوقی"); topBack(c);
            round(c,55,180,1025,1500,30,0xff182329);
            text(c,"از این پرونده چه یاد می‌گیریم؟",950,275,39,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,850);
            text(c,"پس از جمع‌آوری و ارزیابی اطلاعات، برای طرح دعوا باید الزامات قانونی دادخواست را بشناسیم. ماده ۴۸ قانون آیین دادرسی مدنی آغاز رسیدگی را به تقدیم دادخواست منوط می‌کند و ماده ۵۱ اطلاعاتی مانند مشخصات طرفین، خواسته و بهای آن، جهات استحقاق و ادله را در دادخواست مطرح می‌کند.",950,520,31,0xffe5d9c6,true,Layout.Alignment.ALIGN_OPPOSITE,850);
            text(c,"نکته مهم بازی: قانون بعد از کشف مسئله وارد می‌شود؛ بازیکن ابتدا مسئله واقعی را می‌بیند و سپس مفهوم حقوقی را روی آن اعمال می‌کند.",950,900,29,0xffc9ddcf,true,Layout.Alignment.ALIGN_OPPOSITE,850);
            round(c,280,1300,800,1430,25,0xff285e70);
            text(c,"رفتن به تصمیم حقوقی",540,1365,32,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,500);
        }

        void drawQuiz(Canvas c){
            bg(c,0xff11181c); title(c,"تصمیم حقوقی"); topBack(c);
            text(c,"اکنون که مدارک و رابطه آن‌ها را دیدی، کدام اقدام برای شروع رسیدگی لازم است؟",950,250,34,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,880);
            option(c,80,520,"ارسال یک پیام غیررسمی");
            option(c,80,760,"تقدیم دادخواست از مسیر قانونی");
            option(c,80,1000,"فقط تماس تلفنی با طرف مقابل");
            if(phoneUnlocked&&receiptSolved&&cctvSolved){
                round(c,330,1370,750,1490,25,0xff2b654f);
                text(c,"ثبت پاسخ و پایان پرونده",540,1430,30,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,400);
            }
        }

        void option(Canvas c,float x,float y,String s){
            round(c,x,y,x+920,y+150,25,0xff253039); stroke(c,x,y,x+920,y+150,25,0xff6f7b7e,2);
            text(c,s,950,y+75,31,Color.WHITE,true,Layout.Alignment.ALIGN_OPPOSITE,830);
        }

        void drawResult(Canvas c){
            bg(c,0xff0c1514);
            text(c,"پرونده اول به پایان رسید",540,430,54,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,900);
            text(c,"تو ابتدا در محیط پرونده تحقیق کردی، سه مدرک را پیدا کردی، رابطه آن‌ها را دیدی و بعد وارد بخش حقوقی شدی.",540,620,31,0xffd7e2d9,true,Layout.Alignment.ALIGN_CENTER,900);
            round(c,290,920,790,1060,28,0xff245e4b);
            text(c,"بازگشت به کتابخانه",540,990,33,Color.WHITE,true,Layout.Alignment.ALIGN_CENTER,500);
        }

        void pulse(Canvas c,float x,float y,float r){
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6);p.setColor(0xffe4bb69);c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.FILL);
        }

        void drawBitmapFit(Canvas c,Bitmap b,float x,float y,float w,float h){
            if(b==null)return;
            RectF d=new RectF(x-w/2f,y-h,x+w/2f,y);
            c.drawBitmap(b,null,d,p);
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            float x=LX(e.getX()), y=LY(e.getY());
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){
                downX=x;downY=y;downTime=System.currentTimeMillis();draggingPan=false;receiptMoved=false;
                if(screen==CASE_ROOM) return true;
                if(screen==RECEIPT){
                    int picked=-1;
                    if(hit(x,y,20,1240,220,1410))picked=0;
                    else if(hit(x,y,260,1400,440,1570))picked=1;
                    else if(hit(x,y,560,1230,740,1400))picked=2;
                    else if(hit(x,y,810,1420,1000,1590))picked=3;
                    if(picked>=0&&!receiptPlaced[picked]){receiptPiece=picked;receiptPX=x;receiptPY=y;receiptStartX=x;receiptStartY=y;}
                }
                return true;
            }
            if(e.getActionMasked()==MotionEvent.ACTION_MOVE){
                if(screen==CASE_ROOM && Math.abs(x-downX)>12){
                    float delta=x-downX;
                    panX=Math.max(0,Math.min(roomWorldW-1080,panX-delta));
                    downX=x;draggingPan=true;invalidate();
                }
                if(screen==RECEIPT && receiptPiece>=0){
                    if(Math.abs(x-receiptStartX)>10 || Math.abs(y-receiptStartY)>10) receiptMoved=true;
                    receiptPX=x;receiptPY=y;invalidate();
                }
                return true;
            }
            if(e.getActionMasked()!=MotionEvent.ACTION_UP) return true;
            if(screen==RECEIPT && receiptPiece>=0){
                if(x>250&&x<835&&y>380&&y<1150){
                    receiptPlaced[receiptPiece]=true;
                    boolean all=receiptPlaced[0]&&receiptPlaced[1]&&receiptPlaced[2]&&receiptPlaced[3];
                    boolean aligned=receiptRotation[0]%2==0&&receiptRotation[1]%2==0&&receiptRotation[2]%2==0&&receiptRotation[3]%2==0;
                    receiptSolved=all&&aligned;
                    receiptPiece=-1; invalidate(); return true;
                }
                if(!receiptMoved){receiptRotation[receiptPiece]=(receiptRotation[receiptPiece]+1)%4;}
                receiptPiece=-1; invalidate(); return true;
            }
            if(screen==CASE_ROOM && draggingPan){invalidate();return true;}
            handleTap(x,y);
            return true;
        }

        void handleTap(float x,float y){
            if(screen==START){ if(hit(x,y,250,930,830,1180)){screen=LIBRARY;invalidate();} return; }
            if(screen==LIBRARY){
                if(hit(x,y,390,500,690,760)){screen=CASE_INFO;invalidate();} return;
            }
            if(screen==CASE_INFO){
                if(hit(x,y,220,1320,860,1540)){screen=CASE_ROOM;dialogue=0;panX=430;invalidate();return;}
                if(hit(x,y,0,0,180,150)){screen=LIBRARY;invalidate();return;}
            }
            else if(screen==CASE_ROOM){
                if(hit(x,y,320,1560,760,1700)&&dialogue<3){dialogue++;invalidate();return;}
                // world-space bag hotspot: the visible bag on the desk.
                float wx=x+panX;
                if(dialogue>=3 && !bagOpened && wx>640&&wx<870&&y>620&&y<930){bagOpened=true;screen=BAG;invalidate();return;}
                if(bagOpened&&phoneUnlocked&&receiptSolved&&cctvSolved&&hit(x,y,300,1570,780,1730)){screen=EVIDENCE;invalidate();return;}
            }
            else if(screen==BAG){
                if(hit(x,y,120,530,480,900)){screen=PHONE;invalidate();return;}
                if(hit(x,y,600,530,960,900)){screen=RECEIPT;invalidate();return;}
                if(hit(x,y,350,960,730,1260)){screen=CCTV;invalidate();return;}
                if(hit(x,y,260,1430,820,1580)&&phoneUnlocked&&receiptSolved&&cctvSolved){screen=CASE_ROOM;invalidate();return;}
                if(hit(x,y,0,0,180,150)){screen=CASE_ROOM;invalidate();return;}
            }
            else if(screen==PHONE){
                if(hit(x,y,330,620,760,1340)){
                    int d=keypadDigit(x,y);
                    if(d>=0 && phoneCount<4){phoneDigits[phoneCount++]=d; if(phoneCount==4){phoneUnlocked=Arrays.equals(phoneDigits,new int[]{1,8,0,3});}invalidate();return;}
                    if(x>650&&y>1160){phoneCount=Math.max(0,phoneCount-1);invalidate();return;}
                }
                if(hit(x,y,0,0,180,150)){screen=BAG;invalidate();return;}
            }
            else if(screen==RECEIPT){
                if(hit(x,y,0,0,180,150)){screen=BAG;receiptPiece=-1;invalidate();return;}
            }
            else if(screen==CCTV){
                if(hit(x,y,150,1140,930,1260)){cctvSolved=true;invalidate();return;}
                if(hit(x,y,0,0,180,150)){screen=BAG;invalidate();return;}
            }
            else if(screen==EVIDENCE){
                if(hit(x,y,220,1280,860,1490)&&evidence[0]&&evidence[1]&&evidence[2]){screen=REASONING;invalidate();return;}
                if(hit(x,y,0,0,180,150)){screen=CASE_ROOM;invalidate();return;}
            }
            else if(screen==REASONING){if(hit(x,y,220,1450,860,1660)){screen=LESSON;invalidate();return;}if(hit(x,y,0,0,180,150)){screen=EVIDENCE;invalidate();return;}}
            else if(screen==LESSON){if(hit(x,y,240,1260,840,1470)){screen=QUIZ;invalidate();return;}if(hit(x,y,0,0,180,150)){screen=REASONING;invalidate();return;}}
            else if(screen==QUIZ){if(hit(x,y,70,740,1010,930)){screen=RESULT;invalidate();return;}if(hit(x,y,0,0,180,150)){screen=LESSON;invalidate();return;}}
            else if(screen==RESULT){if(hit(x,y,250,880,830,1100)){resetStage();screen=LIBRARY;invalidate();}}
        }

        int keypadDigit(float x,float y){
            int col=Math.round((x-400)/140f), row=Math.round((y-690)/150f);
            if(col<0||col>2||row<0||row>3)return -1;
            if(row==3&&col==1)return 0;
            if(row==3)return -1;
            return row*3+col+1;
        }

        boolean hit(float x,float y,float l,float t,float r,float b){return x>=l&&x<=r&&y>=t&&y<=b;}

        void resetStage(){
            dialogue=0;dialogueAsked=false;bagOpened=false;phoneUnlocked=false;receiptSolved=false;cctvSolved=false;
            evidence=new boolean[3];receiptPlaced=new boolean[4];receiptRotation=new int[4];phoneDigits=new int[4];phoneCount=0;receiptPiece=-1;receiptPX=0;receiptPY=0;panX=430;
        }

        boolean back(){
            if(screen==START)return false;
            switch(screen){
                case LIBRARY:screen=START;break;
                case CASE_INFO:screen=LIBRARY;break;
                case CASE_ROOM:screen=CASE_INFO;break;
                case BAG:screen=CASE_ROOM;break;
                case PHONE:screen=BAG;break;
                case RECEIPT:screen=BAG;break;
                case CCTV:screen=BAG;break;
                case EVIDENCE:screen=CASE_ROOM;break;
                case REASONING:screen=EVIDENCE;break;
                case LESSON:screen=REASONING;break;
                case QUIZ:screen=LESSON;break;
                case RESULT:screen=LIBRARY;break;
                default:screen=START;
            }
            invalidate();return true;
        }
    }
}
