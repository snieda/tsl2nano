package de.tsl2.nano.core.execution;

import java.util.Date;

/**
 * TODO: print a progess bar to the terminal window
 * 
 * @author Tom
 * @version $Revision$
 */
public class ProgressBar {
    int maxCount;
    String prefix;
    int barWidth;
    int textWidth;
    int count;
    int step;
    boolean profile;
    long starttime;

    public ProgressBar(int maxCount) {
        this(maxCount, "", 30, 58, 100, false);
    }

    /**
     * prepares a terminal progress bar maxCount : end of the bar prefix : optional text prefix at the end of the bar
     * barWidth : character count of bar in terminal textWidth: maximum characters to print at the end of the bar
     * lineCount: default: 100 (one output per percent). count of outputs for whole progress profile : default: False,
     * if true, additional profile information like memory and time will be printed
     */
    public ProgressBar(int maxCount, String prefix, int barWidth, int textWidth, int lineCount, boolean profile) {
        this.maxCount = maxCount;
        this.prefix = prefix;
        this.barWidth = barWidth;
        this.textWidth = textWidth;
        this.count = 0;
        this.step = (int) ((maxCount > 99 ? maxCount : lineCount) / (float) lineCount);
        this.profile = profile;
        this.starttime = new Date().getTime();
//        print((barWidth + textWidth) * ' ', end = '\r');
    }

    public void enableProfiling() {
        this.profile = true;
    }

    /** <pre>
    * prints a simple terminal progress bar 
    * comment: optional formatable text at the end. will be concatenated with prefix 
    * args   : optinoal arguments to be formatted(with{})into(prefix+comment)"
    * </pre>     
    */
    public void print(String comment,Object...args) {
        String profMsg;
        
    count+=1;
    if (((count-1) % step) !=0)
        return;
//        if (profile) {
//            profMsg =" ("+(new Date().getTime() - starttime)+" "+str(Profiler.mem())+"MB)";
//                    }
//        else{
//            profMsg=""; 
//            comment=(prefix+comment).format(args)+profMsg lpref=len(prefix);
//        }
//        if(lpref==0|| len(comment) < this.textWidth || lpref > this.textWidth){
//        comment = comment[-textWidth:];
//        }else{
//                comment = comment[0:lpref] + comment[-textWidth + lpref:]
//        }
//        i = count;
//        mx = max(i, maxCount);
//
//        cr = i >= mx ? '\n' : '\r';
//        bar = '='; // fill character
//        l = barWidth;  // progress bar length
//
//        p = (int)(100 * (i / (float)mx));
//        x = 1 + (int)l * (i / (float)mx);
//
//        a = '[' + x * bar;
//        b = ' ' * (l - x);
//        c = '] ' + str(p) + '%';
//        print(a + b + c, comment, end=cr);
    }
}