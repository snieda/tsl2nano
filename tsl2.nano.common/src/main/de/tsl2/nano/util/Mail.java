package de.tsl2.nano.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class Mail {
    private DataOutputStream os = null;
    private BufferedReader is = null;
    private PrintStream out;
    private String charset = ENCODING_UTF8;

    public static final String ENCODING_DE = "iso-8859-1";
    public static final String ENCODING_UTF8 = "utf-8";

    /**
     * constructor
     * 
     * @param out
     * @param charset
     */
    public Mail(PrintStream out, String charset) {
        super();
        this.out = out;
        this.charset = charset;
    }

    public synchronized final void sendEmail(String smtpServer,
            String from, String fromRealName,
            String to, String toRealName,
            String subject, String msg) {
        Socket so = null;
        try {
            if (null == smtpServer || 0 >= smtpServer.length() ||
                null == from || 0 >= from.length() ||
                null == to || 0 >= to.length() ||
                ((null == subject || 0 >= subject.length())
                && (null == msg || 0 >= msg.length()))) {
                throw new IllegalArgumentException("Invalid Parameters for SmtpSimple.sendEmail().");
            }
            if (null == fromRealName || 0 >= fromRealName.length()) {
                fromRealName = from;
            }
            if (null == toRealName || 0 >= toRealName.length()) {
                toRealName = to;
            }
            so = new Socket(smtpServer, 25);
            os = new DataOutputStream(so.getOutputStream());
            is = new BufferedReader(
                new InputStreamReader(so.getInputStream()));
            so.setSoTimeout(10000);
            writeRead(true, "220", null);
            writeRead(true, "250", "HELO " + smtpServer + "\n");
            writeRead(true, "250", "RSET\n");
            writeRead(true, "250", "MAIL FROM:<" + from + ">\n");
            writeRead(true, "250", "RCPT TO:<" + to + ">\n");
            writeRead(true, "354", "DATA\n");
            writeRead(false, null, "To: " + toRealName + " <" + to + ">\n");
            writeRead(false, null, "From: " + fromRealName + " <" + from + ">\n");
            writeRead(false, null, "Subject: " + subject + "\n");
            writeRead(false, null, "Mime-Version: 1.0\n");
            writeRead(false, null, "Content-Type: text/plain; charset=\"" + charset + "\"\n");
            writeRead(false, null, "Content-Transfer-Encoding: quoted-printable\n\n");
            writeRead(false, null, msg + "\n");
            writeRead(true, "250", ".\n");
            writeRead(true, "221", "QUIT\n");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
                if (so != null) {
                    so.close();
                }
            } catch (Exception ex) {
            }
        }
    }

    private final void writeRead(boolean bReadAnswer,
            String sAnswerMustStartWith,
            String sWrite)
            throws Exception {
        if (null != sWrite && 0 < sWrite.length()) {
            out.print(sWrite);
            os.writeBytes(sWrite);
        }
        if (bReadAnswer) {
            String sRd = is.readLine() + "\n";
            out.print(sRd);
            if (null != sAnswerMustStartWith
                && 0 < sAnswerMustStartWith.length()
                && !sRd.startsWith(sAnswerMustStartWith)) {
                throw new IllegalStateException();
            }
        }
    }

    public static void main(String[] args) {
        System.out.println(
            "usage: mail <smtp-server> <from-address> [from-real-name] <to-address> [to-real-name] [subject] [message]\n" +
                "Example:\n" +
                "  Mail mail.web.org MyName@MyProvider.org \"My Realname\" HisName@y.z xyz hello greetings\n");
        if (null == args || 6 > args.length) {
            System.out.println("Error: parameters missing!");
            System.exit(1);
        }
        try {
            Mail mail = new Mail(System.out, ENCODING_DE);
            mail.sendEmail(args[0], args[1], args[2], args[3], args[4], args[5],
                (6 < args.length) ? args[6] : null);
        } catch (Exception ex) {
            System.out.println("Error:\n" + ex);
            System.exit(2);
        }
        System.exit(0);
    }

}