import java.net.ServerSocket;
import java.net.Socket;

@SuppressLint("all")
public class Server {

    public static void main(String[] args)throws Exception{

        while (true){

            ServerSocket ss=new ServerSocket(4096);
            Socket s=ss.accept();

            DataInputStream din=new DataInputStream(s.getInputStream());
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());

            String str;
            str=din.readUTF();
            System.out.println("Data Received: " + str);
            dout.writeUTF("OK");

            dout.flush();
            din.close();
            s.close();
            ss.close();
        }

    }
}