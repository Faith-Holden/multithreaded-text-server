package solution;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;

public class TextFileDirectoryServer {

    private static File directory;
    private static final int LISTENING_PORT = 3000;
    private static final String DIRECTORY_ORIGIN = "src\\resources\\files\\";
    static ArrayBlockingQueue<Socket> connectionQueue;

    public static void main(String[] args){
        directory = new File(DIRECTORY_ORIGIN);

        ServerSocket listener;

        connectionQueue = new ArrayBlockingQueue<>(25);

        PortConnectionThread[] threadArray = new PortConnectionThread[10];
        for(int i = 0; i < threadArray.length; i++){
            threadArray[i] = new PortConnectionThread();
            threadArray[i].start();
        }

        try{
            listener = new ServerSocket(LISTENING_PORT);
            System.out.println("Listening on port " + LISTENING_PORT);
            while (true) {
                Socket connection = listener.accept();
                if(connection.isConnected()){
                    System.out.println("Connected.");
                    connectionQueue.put(connection);
                }
            }
        }catch (Exception e){
            System.out.println("Sorry, the server has shut down.");
            System.out.println("Error: " + e);
            return;
        }
    }

    public static class PortConnectionThread extends Thread{
        Socket connection;

        String messageIn;
        BufferedReader reader;
        PrintWriter outgoing;



        public void run(){
            try{
                 connection = connectionQueue.take();
            }catch (InterruptedException e){
            }

            try{
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()) );
                outgoing = new PrintWriter(connection.getOutputStream());

                messageIn = reader.readLine();

                if(messageIn.equals("INDEX")){
                    getIndex(connection);
                    outgoing.flush();
                    outgoing.close();
                    connection.close();
                }else if(messageIn.startsWith("GET ")){
                    File searchFile = new File (DIRECTORY_ORIGIN+ messageIn.substring(4));

                    if(searchFile.exists()){
                        getFileContents(searchFile,connection);
                    }else{
                        outgoing.println("ERROR");
                        outgoing.println(messageIn.substring(4)+" is not a valid file name.");
                    }
                    outgoing.flush();
                    outgoing.close();
                    connection.close();
                }else{
                    outgoing.println("ERROR");
                    outgoing.println(messageIn + " is not a valid request.");
                    outgoing.flush();
                    outgoing.close();
                    connection.close();
                }
            }catch(IOException e){
            }
        }
    }

    private static void getIndex(Socket connection){
        String[] filesInDirectory;
        if(directory.exists()&&directory.isDirectory()){
            filesInDirectory = directory.list();
        }else{
            return;
        }

        try{
            PrintWriter output = new PrintWriter(connection.getOutputStream());
            for(String fileName : filesInDirectory){
                output.println(fileName);
            }
            output.flush();
            System.out.println("Connection" + connection.getPort() +" closed.");
            connection.close();
        }catch (Exception e){
            System.out.println("An error occurred.");
        };
    }

    private static void getFileContents(File searchFile, Socket connection){
        try{
            BufferedReader lineReader = new BufferedReader(new FileReader(searchFile));
            PrintWriter output = new PrintWriter(connection.getOutputStream());
            lineReader.lines().forEach(output::println);
            lineReader.close();
            output.flush();
            connection.close();
        }catch (IOException e){
            System.out.println("An error occurred.");
        }
    }

}
