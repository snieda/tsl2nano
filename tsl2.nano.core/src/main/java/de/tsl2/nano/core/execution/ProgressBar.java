package de.tsl2.nano.core.execution;

import java.util.Date;

import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.StringUtil;

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

	static final char[] cc = new char[] {' '};
	
	@SuppressWarnings("unused")
	private transient char end;

	public ProgressBar(int maxCount) {
		this(maxCount, "", 30, 58, 100, false);
	}

	/**
	 * <pre>
	 * prepares a terminal progress bar 
	 * maxCount : end of the bar 
	 * prefix : optional
	 * text prefix at the end of the bar 
	 * barWidth : character count of bar in terminal 
	 * textWidth: maximum characters to print at the end of the bar
	 * lineCount: default: 100 (one output per percent). count of outputs for whole progress 
	 * profile : default: False, if true, additional profile information
	 * like memory and time will be printed
	 * </pre>
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
		print(fill(barWidth + textWidth, ' '), end = '\r');
	}

	private String fill(int len, char c) {
		if (len <= 0)
			return "";
		StringBuilder buf = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			buf.append(c);
		}
		return buf.toString();
	}

	public void enableProfiling() {
		this.profile = true;
	}

	public void increase(String comment, Object... args) {
		print(comment, args);
	}
		/**
	 * <pre>
	 * prints a simple terminal progress bar - count of steps will be increased 
	 * comment: optional formatable text at the end. will be concatenated with prefix 
	 * args   : optinoal arguments to be formatted(with{})into(prefix+comment)"
	 * </pre>
	 */
	public void print(String comment, Object... args) {
		print(++count, comment, args);
	}
	public void print(int count, String comment, Object... args) {
		String profMsg, a, b, c;
		int i, mx, lpref = 0, p, l, x;
		char bar, cr;

		if (((count - 1) % step) != 0)
			return;
		if (profile) {
			profMsg = " (" + (new Date().getTime() - starttime) + " " + NumberUtil.amount(Profiler.getUsedMem()) + ")";
		} else {
			profMsg = "";
			if (comment.contains("%"))
				comment = String.format(prefix + comment, args) + profMsg;
			else
				comment = prefix + comment +StringUtil.concat(cc, args) + profMsg;
			lpref = prefix.length();
		}
		if (lpref == 0 || comment.length() < this.textWidth || lpref > this.textWidth) {
			comment = substringFromRight(comment, textWidth);
		} else {
			comment = comment.substring(0, lpref) + comment.substring(-textWidth + lpref);
		}
		i = count;
		mx = Math.max(i, maxCount);

		cr = i >= mx ? '\n' : '\r';
		bar = '='; // fill character
		l = barWidth; // progress bar length

		p = (int) (100 * (i / (float) mx));
		x = 1 + (int) (l * (i / (float) mx));

		a = '[' + fill(x, bar);
		b = fill(l - x, ' ');
		c = "] " + p + '%';
		print_(a + b + c + comment, end = cr);
	}

	public boolean isFinished() {
		return count >= maxCount;
	}
	public void setFinished() {
		count = maxCount;
	}
	
	/**
	 * @param txt source text
	 * @param indexFromRight will be subtracted from txt.length()
	 * @return substring with indexFromRight characters starting at txt end.
	 */
	public static String substringFromRight(String txt, int indexFromRight) {
		int len = txt.length();
		return indexFromRight > len ? txt : txt.substring(len - indexFromRight);
	}

	void print_(Object txt) {
		print_(txt, '\r');
	}

	protected void print_(Object txt, char end) {
		System.out.print(txt.toString() + end);
	}
}