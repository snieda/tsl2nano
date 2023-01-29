package de.tsl2.nano.core.execution;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import de.tsl2.nano.core.util.CLI;
import de.tsl2.nano.core.util.NumberUtil;
import de.tsl2.nano.core.util.StringUtil;
import de.tsl2.nano.core.util.CLI.Color;

/**
 * print a progess bar to the terminal window
 * 
 * @author Tom
 * @version $Revision$
 */
public class ProgressBar {
	private static final String STARTING = "..starting..";
	protected static final int PERC_WIDTH = 8; // '[' + '] 100% '
	protected int maxCount;
	protected String prefix;
	protected int barWidth;
	protected char bar;
	protected int textWidth;
	protected AtomicInteger count = new AtomicInteger();
	protected int step;
	protected boolean profile;
	protected long starttime;
	protected boolean done;

	static final char[] CC = new char[] {' '};
	
	public ProgressBar() {
		this(100);
	}
	public ProgressBar(int maxCount) {
		this(maxCount, "", 30, '=', 58, 100, false);
	}

	public ProgressBar(int maxCount, String prefix, int barWidth, int textWidth, int stepCount, boolean profile) {
		this(maxCount, prefix, barWidth, '=', textWidth, stepCount, profile);
	}
	/**
	 * prepares a terminal progress bar
	 * 
	 * @param maxCount : finish of progress
	 * @param prefix : optional text prefix at the end of the bar
	 * @param barWidth : character count of bar (without text) in terminal
	 * @param barChar : character to fill the bar with
	 * @param textWidth : maximum characters to print at the end of the bar
	 * @param stepCount: default: 100 (one output per percent). count of outputs for
	 *                   whole progress
	 * @param profile : default: false, if true, additional profile information like
	 *          memory and time will be printed
	 */
	public ProgressBar(int maxCount, String prefix, int barWidth, char barChar, int textWidth, int stepCount, boolean profile) {
		this.maxCount = maxCount;
		this.prefix = prefix;
		this.barWidth = barWidth;
		this.bar = barChar;
		this.textWidth = textWidth;
		this.count.set(0);
		this.step = stepCount > maxCount ? 1 : (int) (maxCount / stepCount);
		this.profile = profile;
		this.starttime = new Date().getTime();
		print(0, STARTING + fill(barWidth + textWidth - STARTING.length(), ' '));
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

	/** convenience method delegating to {@link #print(String, Object...)} increasing the progress count */
	public void increase(String comment, Object... args) {
		print(comment, args);
	}
		/**
	 * prints a simple terminal progress bar - count of steps will be increased 
	 * @param comment: optional formatable text at the end. will be concatenated with prefix 
	 * @param args   : optional arguments to be formatted(with{})into(prefix+comment)"
	 */
	public void print(String comment, Object... args) {
		print(count.getAndIncrement(), comment, args);
	}
	public void print(int count, String comment, Object... args) {
		String profMsg, a, b, c;
		int mx, p, x;
		char cr;

		if (done)
			return;
		if (count >= maxCount) {
			done = true;
			print_(comment + CLI.tag(" -> done (" + (System.currentTimeMillis() - starttime) + " ms)", Color.GREEN), '\n');
			return;
		}
		if (((count - 1) % step) != 0)
			return;
		profMsg = profile ? profMsg = " (" + (new Date().getTime() - starttime) + " " + NumberUtil.amount(Profiler.getUsedMem()) + ")" : "";
		if (comment.contains("%") && args.length > 0)
			comment = String.format(prefix + comment, args) + profMsg;
		else
			comment = prefix + comment + (args.length > 0 ? StringUtil.concat(CC, args) : "") + profMsg;
		
		comment = comment.length() > textWidth ? ".." + substringFromRight(comment, textWidth-2) : comment;
		mx = Math.max(count, maxCount);

		cr = count >= mx ? '\n' : '\r'; //end

		p = (int) (100 * (count / (float) mx));
		x = (int) (barWidth * (count / (float) mx));

		a = '[' + fill(x, bar);
		b = fill(barWidth - x, ' ');
		c = "] " + p + "% ";
		print_(a + b + c + comment, /*end = */cr);
	}

	public boolean isFinished() {
		return count.get() >= maxCount;
	}
	public void setFinished() {
		count.set(maxCount);
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