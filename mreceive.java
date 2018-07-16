/*

  Multicast testing program: Receiving
  Author: Chun Fung, Wong

  Last Updated: 5-Sep-2016
  Version 2.0

*/

import java.net.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import static java.lang.System.out;

public class mreceive extends JFrame
{
  private static java.util.Timer timer, rpkt_thread;
  private static final int IP_PKT_SIZE_MAX = 1518;
  private static int pps=0;
  private static String group = "239.10.10.1";
  private static String log = "";
  private static int port = 5000;
  private static int check_seqno=0, seqno=0, totalPktLost=0, totalPktRcvd=0;
  private static boolean receiving = false, OUT_SEQ = false;
  private static Color RED, GREEN;

  private static JTextField t_pps, t_pktloss, t_pktrcvd, t_group, t_port;
  private static JTextArea messageLog;
  private static JButton start, stop, reset, clear; 
  private static JLabel l_pps, l_pktloss, l_pktrcvd, l_group, l_port, l_int;
  private static JComboBox n_interface;

  private static MulticastSocket s;
  private static byte [] buf;
  private static DatagramPacket p;
  private static NetworkInterface netint;

  public mreceive()
  {
    super ("Multicast Receive ver 2.0");
    setIconImage ( new ImageIcon("mcast.gif").getImage());

    String[] netString = {""};
    JComboBox<String> n_interface = new JComboBox<String> (netString);

    buf = new byte [IP_PKT_SIZE_MAX];

    // List out available Network Interfaces  
    try
    {
      Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
      for (NetworkInterface netint : Collections.list(nets))
      {
        //out.printf("Name: %s\n", netint.getName());
        n_interface.addItem(netint.getName());
      }
    } catch  ( Exception ex ) 
    {
    }

    class ButtonListener implements ActionListener
    {
       public ButtonListener(){}

       public void actionPerformed ( ActionEvent e)
       {
          if ("start".equals(e.getActionCommand()))
          {
             group = t_group.getText(); 
             port = Integer.parseInt(t_port.getText());
             if (port < 1024 || port > 65535) 
             { port = 5000; t_port.setText("5000"); }
             start.setEnabled(false);
             stop.setEnabled(true);
             Calendar now = Calendar.getInstance();
             log += "Multicast Started at: " + now.get(Calendar.DAY_OF_MONTH) + "-" + (now.get(Calendar.MONTH) + 1) + "-" + now.get(Calendar.YEAR) + " " + now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE) + ":" + now.get(Calendar.SECOND) + "\n";
             messageLog.setText(log);
             try
             {
               s = new MulticastSocket(port);

               Enumeration<NetworkInterface> nic = NetworkInterface.getNetworkInterfaces();
               for (NetworkInterface nc : Collections.list(nic))
               {
                 String x = String.valueOf(n_interface.getSelectedItem());
                 if ( x.equals (nc.getName()))
	           s.setNetworkInterface(nc);
               }
               s.joinGroup(InetAddress.getByName(group));
             } catch ( Exception ex ) {}

             timer = new java.util.Timer();
             rpkt_thread = new java.util.Timer();
             timer.scheduleAtFixedRate(new Schedule(), 0, 1000);
             rpkt_thread.scheduleAtFixedRate(new Rpkt(), 0, 1);
             receiving = true;
          }
          else if ("stop".equals(e.getActionCommand()))
          {
             receiving = false;
             start.setEnabled(true);
             stop.setEnabled(false);
             try
             {
               s.leaveGroup(InetAddress.getByName(group));
             } catch ( Exception ex ) {}
             Calendar now = Calendar.getInstance();
             log += "Multicast Stopped at: " + now.get(Calendar.DAY_OF_MONTH) + "-" + (now.get(Calendar.MONTH) + 1) + "-" + now.get(Calendar.YEAR) + " " + now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE) + ":" + now.get(Calendar.SECOND) + "\n";
             messageLog.setText(log);
             t_pps.setBackground ( Color.lightGray );
             messageLog.setBackground ( Color.lightGray );
             t_pktloss.setBackground ( Color.lightGray );
             t_pktrcvd.setBackground ( Color.lightGray );
             check_seqno = 0;
             seqno = 0;
             timer.cancel();
             rpkt_thread.cancel();
          }
          else if ("reset".equals(e.getActionCommand()))
          {
             check_seqno = 0;
             seqno = 0;
             Calendar now = Calendar.getInstance();
             log += "Sequence number reset at: " + now.get(Calendar.DAY_OF_MONTH) + "-" + (now.get(Calendar.MONTH) + 1) + "-" + now.get(Calendar.YEAR) + " " + now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE) + ":" + now.get(Calendar.SECOND) + "\n";
             messageLog.setText(log);
          }
          else 		// clear
          {
             check_seqno = 0;
             seqno = 0;
             totalPktLost = 0;
             totalPktRcvd = 0;
             t_pps.setBackground ( Color.lightGray );
             messageLog.setBackground ( Color.lightGray );
             t_pktloss.setBackground ( Color.lightGray );
             t_pktrcvd.setBackground ( Color.lightGray );
             Calendar now = Calendar.getInstance();
             log = "State cleared at: " + now.get(Calendar.DAY_OF_MONTH) + "-" + (now.get(Calendar.MONTH) + 1) + "-" + now.get(Calendar.YEAR) + " " + now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE) + ":" + now.get(Calendar.SECOND) + "\n";
             messageLog.setText(log);
          }
       }
    } // End ButtonListener
  
    addWindowListener ( new WindowAdapter()
    {
       public void windowClosing ( WindowEvent e)
       {
          mreceive.this.dispose();
          System.exit(0);
       }
    });

    messageLog = new JTextArea();
    messageLog.setLineWrap(true);
    messageLog.setWrapStyleWord(true);
    messageLog.setEditable(false);

    JScrollPane logpane = new JScrollPane(messageLog);
    logpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    logpane.setPreferredSize(new Dimension(400, 200));
    logpane.setBorder(BorderFactory.createCompoundBorder(
                       BorderFactory.createCompoundBorder(
                          BorderFactory.createTitledBorder("Message Log"),
                          BorderFactory.createEmptyBorder(3,3,3,3)),
                       logpane.getBorder()));
    
    JPanel panel = new JPanel ( new GridLayout (6, 2) );

    l_pps = new JLabel("Rate (pps)");
    l_pktloss = new JLabel("Packet loss");
    l_pktrcvd = new JLabel("Packet received");
    l_group = new JLabel("Receive Group");
    l_port = new JLabel("Port");
    l_int = new JLabel("Interface");
    t_pps = new JTextField("0");
    t_pps.setEditable(false);
    t_pktloss = new JTextField("0");
    t_pktloss.setEditable(false);
    t_pktrcvd = new JTextField("0");
    t_pktrcvd.setEditable(false);
    t_group = new JTextField("239.10.10.1");
    t_port = new JTextField("5000");
  
    panel.add(l_group);
    panel.add(t_group);
    panel.add(l_port);
    panel.add(t_port);
    panel.add(l_pktrcvd);
    panel.add(t_pktrcvd);
    panel.add(l_pktloss);
    panel.add(t_pktloss);
    panel.add(l_pps);
    panel.add(t_pps);
    panel.add(l_int);
    panel.add(n_interface);

    JPanel button = new JPanel ();
    button.setLayout( new FlowLayout(FlowLayout.CENTER));

    start = new JButton("Start");
    start.addActionListener( new ButtonListener() );
    start.setActionCommand("start");
    button.add( start );

    stop = new JButton("Stop");
    stop.addActionListener( new ButtonListener() );
    stop.setActionCommand("stop");
    button.add( stop );
    stop.setEnabled(false);

    reset = new JButton("Reset");
    reset.addActionListener( new ButtonListener() );
    reset.setActionCommand("reset");
    button.add( reset );

    clear = new JButton("Clear");
    clear.addActionListener( new ButtonListener() );
    clear.setActionCommand("clear");
    button.add( clear );

    Container cp = getContentPane();
    cp.add (logpane, BorderLayout.CENTER);
    cp.add (panel, BorderLayout.NORTH);
    cp.add (button, BorderLayout.SOUTH);
             
    t_pps.setBackground ( Color.lightGray );
    messageLog.setBackground ( Color.lightGray );
    t_pktloss.setBackground ( Color.lightGray );
    t_pktrcvd.setBackground ( Color.lightGray );

    pack();
    setVisible(true);

    RED = new Color(255, 158, 158);
    GREEN = new Color(125, 249, 150);
   
    
  }

  // Timer thread to count PPS and packet loss at interval of 1 second
  class Schedule extends TimerTask
  {
    public void run()
    { 
       t_pps.setText(Integer.toString(pps)); 
       t_pktloss.setText(Integer.toString(totalPktLost)); 
       t_pktrcvd.setText(Integer.toString(totalPktRcvd)); 

       // System.out.println(pps + " pps, Total packet lost:" + totalPktLost);
       // System.out.println("Seq no: " + check_seqno + ", " + seqno);
       if(pps == 0 && check_seqno !=0)
       {
         if (receiving)
         {
           t_pps.setBackground ( RED );
           messageLog.setBackground ( RED );
           t_pktloss.setBackground ( RED );
           t_pktrcvd.setBackground ( RED );
           Calendar now = Calendar.getInstance();
           log += "Timeout at: " + now.get(Calendar.DAY_OF_MONTH) + "-" + (now.get(Calendar.MONTH) + 1) + "-" + now.get(Calendar.YEAR) + " " + now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE) + ":" + now.get(Calendar.SECOND) + "\n";
           messageLog.setText(log);
         }
       }
       else if (OUT_SEQ)
       {
         t_pktloss.setBackground ( RED );
         messageLog.setBackground ( RED );
         Calendar now = Calendar.getInstance();
         log += "Incorrect sequence: " + now.get(Calendar.DAY_OF_MONTH) + "-" + (now.get(Calendar.MONTH) + 1) + "-" + now.get(Calendar.YEAR) + " " + now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE) + ":" + now.get(Calendar.SECOND) + "\n";
         log += "From " + check_seqno + " to " + seqno +"\n";
         messageLog.setText(log);
       }
       else if (check_seqno!=0)
       {
         t_pps.setBackground ( GREEN );
         messageLog.setBackground ( GREEN );
         t_pktrcvd.setBackground ( GREEN );
         if (totalPktLost==0) t_pktloss.setBackground ( GREEN );
       }
       pps=0;
       if (!receiving)
       {
         t_pps.setBackground ( Color.lightGray );
         messageLog.setBackground ( Color.lightGray );
         t_pktloss.setBackground ( Color.lightGray );
         t_pktrcvd.setBackground ( Color.lightGray );
       }
     } //End run()
   }

   class Rpkt extends TimerTask
   {
      
      Rpkt()
      {
        p = new DatagramPacket(buf, buf.length);
      }

      public void run()
      {
         try
         {
           // Continue keep receive packets, corresponding flags updated by timer thread
            if (receiving)
            {
             buf = new byte[IP_PKT_SIZE_MAX];
             //p = new DatagramPacket(buf, buf.length);
             p.setData(buf);
             s.receive(p);

             // System.out.println("Packet length:" + p.getLength());
              
             // Extract sequence number from newly arrived packet
             ByteArrayInputStream bi = new ByteArrayInputStream(buf);
             DataInputStream di = new DataInputStream(bi);
             seqno = di.readInt();

             // Account for lost packet
             if(check_seqno <= 0 && seqno > 0)
             {
               check_seqno = seqno;
             }
             else if(check_seqno !=0 && seqno - check_seqno != 1)
             {
              // System.out.println("Lost packet:" + (seqno - check_seqno));
              if (seqno > check_seqno)
              {
               totalPktLost += seqno - check_seqno - 1;
               check_seqno = seqno;
               t_pktloss.setBackground ( RED );
              }
              else if (seqno < check_seqno)
              {
                t_pktrcvd.setBackground ( RED );
                messageLog.setBackground ( RED );
                OUT_SEQ = true;
                Calendar now = Calendar.getInstance();
                log += "Incorrect sequence: " + now.get(Calendar.DAY_OF_MONTH) + "-" + (now.get(Calendar.MONTH) + 1) + "-" + now.get(Calendar.YEAR) + " " + now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE) + ":" + now.get(Calendar.SECOND) + "\n";
                log += "From " + check_seqno + " to " + seqno +"\n";
                messageLog.setText(log);
              }
             }
             else if(seqno - check_seqno == 1)
             {
               check_seqno = seqno;
               t_pktrcvd.setBackground( GREEN );
               messageLog.setBackground( GREEN );
               OUT_SEQ = false;
             }
             totalPktRcvd++;
             pps++;
            }
        } catch (Exception e){ System.out.println (e); }
      }
   }
 
   public static void main(String[] args)
   {
       new mreceive();

   } //End main()
}
