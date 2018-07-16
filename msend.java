/*
 
  Multicast testing program: Sending
  Author: Chun Fung, Wong

  Last Updated: 5-Sep-2016
  Version 2.0

*/

//import sun.net.*;
import java.net.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import static java.lang.System.out;

public class msend extends JFrame
{
  private java.util.Timer timer;
  private static final int IP_PKT_SIZE_MAX = 65000;
  private static int seqno = 0;
  private static int packet_length = 0;
  private static int port = 5000;
  private static int ttl = 15;
  private static int pps = 10;
  private static String group = "239.10.10.1";
  private static boolean sending = false;
  private static double packet_gap = 0.0;
  private static double gap = 0.0;
  private static int timer_count = 0;
 
  private JTextField p_size, p_port, p_group, p_rate, p_ttl;
  private JButton start, stop;
  private JLabel l_size, l_port, l_group, l_rate, l_ttl, l_int;
  private JPanel panel, button;
  private JComboBox n_interface;

  private static MulticastSocket s;
  private static NetworkInterface netint;

  public msend()
  {  
    super ("Multicast Send ver 2.0");
    setIconImage( new ImageIcon("mcast.gif").getImage());

    String[] netString = {""};
    n_interface = new JComboBox(netString);

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
        private String t;

        public ButtonListener(String text)
        {
          this.t = text;
        }

        public void actionPerformed ( ActionEvent e )
        {
          if("start".equals(e.getActionCommand()))
          {
            start.setEnabled(false);
            stop.setEnabled(true);
            port = Integer.parseInt(p_port.getText());
            pps = Integer.parseInt(p_rate.getText());
            ttl = Integer.parseInt(p_ttl.getText());
            packet_length = Integer.parseInt(p_size.getText());
            group = p_group.getText();
            if (port < 1025 || port > 65535) 
            { if (port < 1025) { port = 1025; p_port.setText("1025");}
              else { port = 65535; p_port.setText("65535");} }
            if (pps < 0 || pps > 1000000) 
            { if (pps < 0) { pps = 0; p_rate.setText("0");}
              else { pps = 1000000; p_rate.setText("1000000");} }
            if (ttl < 1 || ttl > 127) 
            { if (ttl < 1) { ttl = 1; p_ttl.setText("1");}
              else { ttl = 127; p_ttl.setText("127");} }
            if (packet_length < 4 || packet_length > IP_PKT_SIZE_MAX) 
            { if (packet_length < 4){ packet_length = 4; p_size.setText("4");}
              else { packet_length = IP_PKT_SIZE_MAX; p_size.setText((new Integer(IP_PKT_SIZE_MAX)).toString());} }
            packet_gap = 1000.0/(double)pps;
            timer_count = 0;
            try
            {
              s = new MulticastSocket(port);
              Enumeration<NetworkInterface> nic = NetworkInterface.getNetworkInterfaces();
               for (NetworkInterface nc : Collections.list(nic))
               {
                 String x = String.valueOf(n_interface.getSelectedItem());
                 if ( x.equals (nc.getName()))
                 {
                   out.printf ("Interface: %s\n", x);
                   s.setNetworkInterface(nc);
                 }
               }
            } catch ( Exception ex ) {}
            sending = true;
          }
          else
          {
            start.setEnabled(true);
            stop.setEnabled(false);
            sending = false;
          }
        }
     }

    addWindowListener ( new WindowAdapter() 
    {
       public void windowClosing ( WindowEvent e )
       {
           msend.this.dispose();
           System.exit(0);
       }
     } );


     panel = new JPanel ( new GridLayout(6,2) );

     l_group = new JLabel ("Multicast Group");
     p_group = new JTextField(group);
     l_port = new JLabel("UDP port");
     p_port = new JTextField("5000");
     l_size = new JLabel("Packet size (payload) ");
     p_size = new JTextField("256");
     l_rate = new JLabel("Rate (pps) ");
     p_rate = new JTextField("10");
     l_ttl = new JLabel("TTL");
     p_ttl = new JTextField("15");
     l_int = new JLabel("Interface");

     panel.add(l_group);
     panel.add(p_group);
     panel.add(l_port);
     panel.add(p_port);
     panel.add(l_size);
     panel.add(p_size);
     panel.add(l_rate);
     panel.add(p_rate);
     panel.add(l_ttl);
     panel.add(p_ttl);
     panel.add(l_int);
     panel.add(n_interface);
    
     button = new JPanel();
     button.setLayout( new FlowLayout(FlowLayout.CENTER) );

     start = new JButton("Start");
     start.addActionListener( new ButtonListener("Start") );
     start.setActionCommand("start");
     button.add( start );

     stop = new JButton("Stop");
     stop.addActionListener( new ButtonListener("Stop") );
     stop.setActionCommand("stop");
     button.add( stop );
     
     Container cp = getContentPane();
     cp.add (panel, BorderLayout.NORTH);
     cp.add (button, BorderLayout.SOUTH);

    stop.setEnabled(false); 
    pack();
    setVisible( true );

    timer = new java.util.Timer();

    // Schedule timer thread for sending out fixed number of packets in 1 second
    timer.scheduleAtFixedRate(new Schedule(), 0, 1);

  } // End send()

  class Schedule extends TimerTask
  {
    byte[] buf;
    DatagramPacket p;

    public void run()
    {
     timer_count++;

     if(sending)
     {
      try
      {

        byte [] seqbuf = new byte[4];
        buf = new byte[packet_length];

        ByteArrayOutputStream bo = new ByteArrayOutputStream(packet_length);
        DataOutputStream doutput = new DataOutputStream(bo);

        p = new DatagramPacket(buf, buf.length, InetAddress.getByName(group), port);

        // System.out.println(seqno + " " + gap + " " + packet_gap + " " + timer_count);
        while (gap < timer_count) 
        {
          // Fill up packet with arbitary data
          // for (int i=0; i<packet_length; i++) buf[i] = (new Integer(i)).byteValue();

          seqno++;

          // Convert sequence number to a 4-byte array
          doutput.writeInt(seqno);
          seqbuf = bo.toByteArray();

          // Insert sequence number to the buffer
          buf[0] = seqbuf[0];
          buf[1] = seqbuf[1];
          buf[2] = seqbuf[2];
          buf[3] = seqbuf[3];

          // Create packet and send out
          //p = new DatagramPacket(buf, buf.length, InetAddress.getByName(group), port);
          p.setData(buf);
          
          s.setTimeToLive((byte)ttl);
          s.send(p);
          gap += packet_gap;
        }
       } catch (Exception e){}
      }
      if (timer_count == 1000) { timer_count = 0; gap = 0.0; }
    } // End run()
  }

  public static void main(String[] args)
  {
    new msend();
  }
}
