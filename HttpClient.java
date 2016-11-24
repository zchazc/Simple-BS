
import java.net.*;
import java.io.*;

public class HttpClient
{
	public static void main(String[] args) throws IOException {
		//读取用户指令，判断是否合法
		if (args.length != 2) {
			System.out.println("Usage: java HttpClient <host_name> <port_number>");
			System.exit(1);
		}
    	
        Socket clientSocket = null;
        
        PrintWriter requestPrintWriter = null;
        
        BufferedReader responseBufferedReader = null;
        
        DataInputStream responseDataInputStream = null;
        
        int contentLength = 0;
        
        final byte[] fileDataPart = new byte[1024];
        
        //建立socket连接服务器，并读取用户的请求输入
        final InetAddress addressbyHostName = InetAddress.getByName(args[0]);
        
        final int portNum = Integer.valueOf(args[1]);
        
        
        final BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Please input file name: ");
        final String fileName;
        if ((fileName = userInput.readLine()) != null) {
            try {
                clientSocket = new Socket(addressbyHostName, portNum);
                requestPrintWriter = new PrintWriter(clientSocket.getOutputStream(), true);
                responseBufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            }catch(BindException ex){
            	System.err.println("The port:"+"portNum"+" is in use.");
                System.exit(1);
            	
            }catch (SocketTimeoutException ex) {
            	System.out.println("A timeout has occurred on a socket read or accept.");
            	System.exit(1);
			}
            catch (UnknownHostException ex) {
                System.err.println("Don't know about host: " + args[0]);
                System.exit(1);
            }
            catch (IOException ex) {
                System.err.println("Couldn't get I/O for the connection to: " + args[0]);
                System.exit(1);
            }
            
            System.out.println("\n====================================\n");
            
            //发送错误HTTP1.0请求
            final String requestFile = "/" + fileName;
            System.out.println("Send out a bad request...... ");
            System.out.print("WrongMethod " + requestFile + " HTTP/1.0\r\n\r\n");
            requestPrintWriter.print("WrongMethod " + requestFile + " HTTP/1.0\r\n\r\n");
            requestPrintWriter.flush();
            //接收服务器响应，并关闭联接
            System.out.println("Receive response from server .....");
            final String responseHeadErr;
            if ((responseHeadErr = responseBufferedReader.readLine()) == null) {
                System.err.println("Couldn't read response from server.");
            }
            else if (responseHeadErr.equalsIgnoreCase("HTTP/1.0 400 Bad Request")) {
                System.out.println("\t" + responseHeadErr);
            }
            else {
                System.out.println("\t Wrong response from the server:\n");
                System.out.println("\t" + responseHeadErr);
            }
            System.out.println("\n====================================\n");
            if(clientSocket!=null)
            clientSocket.close();
            if(requestPrintWriter!=null)
            requestPrintWriter.close();
            if(responseBufferedReader!=null)
            responseBufferedReader.close();
            
            //再次建立连接发送正确HTTP1.0请求
            try {
                clientSocket = new Socket(addressbyHostName, portNum);
                requestPrintWriter = new PrintWriter(clientSocket.getOutputStream(), true);
                responseDataInputStream = new DataInputStream(clientSocket.getInputStream());
             
            }catch(BindException ex){
            	System.err.println("The port:"+"portNum"+" is in use.");
                System.exit(1);
            	
            }catch (SocketTimeoutException ex) {
            	System.out.println("A timeout has occurred on a socket read or accept.");
            	System.exit(1);
			}catch (UnknownHostException ex) {
                System.err.println("Don't know about host: " + args[0]);
                System.exit(1);
            }
            catch (IOException ex) {
                System.err.println("Couldn't get I/O for the connection to: " + args[0]);
                System.exit(1);
            }
            //发送正确请求内容
            System.out.println("Send out the right request..... ");
            System.out.println("GET " + requestFile + " HTTP/1.0");
            requestPrintWriter.print("GET " + requestFile + " HTTP/1.0\r\n");
            requestPrintWriter.print("Host: " + args[0] + "\r\n\r\n");
            requestPrintWriter.flush();
           
            //判断响应内容并做处理
            System.out.println("\nReceive response from server .....");
            String responseInfo;
            if ((responseInfo = responseDataInputStream.readLine()) == null) {
                System.err.println("Couldn't read response from server.");
            }
            //与后面的等价.equalsIgnoreCase("HTTP/1.0 200 OK") || responseInfo.equalsIgnoreCase("HTTP/1.1 200 OK")
            else if (responseInfo.toLowerCase().matches("http/1.[01] 200 ok")) {
                System.out.println("\t" + responseInfo);
                while (!responseInfo.isEmpty()) {
                    responseInfo = responseDataInputStream.readLine();
                    //判断是否符合“Content-Length:空白字符（0个以上）数字（1个以上）”
                    if (responseInfo.matches("Content-Length:\\s*\\d+")) {
                    	//将所有非数字字符替换为空字符
                        contentLength = Integer.parseInt(responseInfo.replaceAll("[^\\d]", ""));
                    }
                    System.out.println("\t" + responseInfo);
                }
                FileOutputStream fileOutputStream = null;
                try {
                	//存储响应内容
                	System.out.println("Please input save-file name:");
                	final String saveFileName= userInput.readLine();
                    fileOutputStream = new FileOutputStream(new File(".//" + saveFileName));
                    int realGetDataNum;
                    for (int i = contentLength; i > 0; i -= realGetDataNum) {
                    	//i剩余文件内容长度小于1024则直接读取i长度的内容并发送，否则读取1024长度内容并发送
                        if (i <= 1024) {
                            realGetDataNum = responseDataInputStream.read(fileDataPart, 0, i);
                            fileOutputStream.write(fileDataPart, 0, realGetDataNum);
                        }
                        else {
                            realGetDataNum = responseDataInputStream.read(fileDataPart, 0, 1024);
                            fileOutputStream.write(fileDataPart, 0, realGetDataNum);
                        }
                        fileOutputStream.flush();
                    }
                    fileOutputStream.close();
                    
                    
                }
                catch (IOException ex) {
                    System.err.println("Couldn't open and write to the output file: " + ex);
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                }
            }
            //与后面的等价.equalsIgnoreCase("HTTP/1.0 404 Not Found") || responseInfo.equalsIgnoreCase("HTTP/1.1 404 Not Found")
            else if (responseInfo.toLowerCase().matches("http/1.[01] 404 not found")) {
                System.out.println("\t" + responseInfo);
                System.out.println("\n\nFile does not exist on the server!\n");
            }
            else {
                System.out.println("\t Unkonwn responses from the server!\n");
                System.out.println("\t" + responseInfo);
            }
            System.out.println("\n");
            System.out.println("Closing I/O and quiting...");
            
            
            
            if(requestPrintWriter!=null)
            requestPrintWriter.close();
            if(responseDataInputStream!=null)
            responseDataInputStream.close();
            if(userInput!=null)
            userInput.close();
            if(clientSocket!=null)
            clientSocket.close();
            System.exit(0);
        }
    }
}
