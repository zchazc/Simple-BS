

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class HttpServer
{
    
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		
		//判断传入参数
		if (args.length != 1) {
			System.out.println("Usage: java HttpServer <port_number>");
			System.exit(1);
		}
		
		
		
		//监听socket
        ServerSocket serverSocket = null;
        //文字方式的输出流(HTTP响应)
        PrintWriter responsePrintWriter = null;
        //数据方式的输出流(HTTP响应)
        DataOutputStream responseDataOutputStream = null;
        //字符缓冲区(HTTP请求)
        BufferedReader requestBufferedReader = null;
        //用来建立新请求的socket
        Socket requestSocket = null;
        //用来读取请求文件
        File requestFile = null;
        //请求文件的输入流
        FileInputStream fileInputStream = null;
        //读取系统输入(控制台输入)
        Scanner userInput=new Scanner(System.in);
        //请求次数
        int connectionCount = 0;
        while (true) {
		//读取用户指令，判断是否合法
		//创建serversocket监听
    	  
		try {
			serverSocket = new ServerSocket(Integer.valueOf(args[0]));
		} catch (NumberFormatException ex) {
			System.out.println("Couldn't create server socket: WrongPortNumber");
		} catch (IOException ex) {
			System.err.println("Couldn't create server socket: " + ex);
			
		} 
		//判断serverSocket是否创建成功，若前代码块抛出异常，则serversocket为null
		if(serverSocket==null){
			System.out.println("Please try again or exit:");
			args[0]=userInput.next();
			if("exit".equalsIgnoreCase(args[0]))System.exit(1);
		}else
			break;
		
	}
		
        
        //持续等待请求并创建连接
        while (true) {
            try {
            	System.out.println();
                System.out.println(connectionCount + " connections served. Accepting new client...");
                System.out.println("\n========================================\n");
                //响应请求创建一个socket对象，accept方法为阻塞方法
                requestSocket = serverSocket.accept();
                //得到请求socket对象的输入流
                requestBufferedReader = new BufferedReader(new InputStreamReader(requestSocket.getInputStream()));
                //得到请求socket对象输出流并做好字符转字节的处理
                final OutputStream outputStream = requestSocket.getOutputStream();
                responsePrintWriter = new PrintWriter(outputStream, true);
                responseDataOutputStream = new DataOutputStream(new BufferedOutputStream(outputStream));
            }
            catch (IOException ex) {
                System.err.println("Failed to create I/O: " + ex);
                System.exit(1);
            }
            
            //读取请求内容，判断是否符合HTTP协议并回复响应
            try {
                final String requestHead;
                if ((requestHead = requestBufferedReader.readLine()) != null) {
                System.out.println("Request Info:\n"+requestHead);
                
                	while (requestBufferedReader.ready()) {
						System.out.println(requestBufferedReader.readLine());
					
					}
                	//读取请求头，判断是否符合HTTP协议，不符合则回复HTTP/1.0 400 Bad Request
                    final String[] requestHeadInfos = requestHead.split("\\s+", 10);
                    String reString=null;
                    // != 3||requestHeadInfos[1].charAt(0) != '/'
                	//||(!requestHeadInfos[2].equalsIgnoreCase("http/1.0") 
                    //&& !requestHeadInfos[2].equalsIgnoreCase("http/1.1"))
                    //符合“英文字符（1个以上）+空格+/+任意字符或空格（1个以上）+http/1.1或1.0”的字符
                    if (!requestHead.toLowerCase().matches("[A-z]+ /[\\S ]+ http/1.[01]")) {
                    	reString="Wrong Request:"+requestHead;
                        responsePrintWriter.print("HTTP/1.0 400 Bad Request\r\n");
                        responsePrintWriter.print("Content-Length: " + reString.length() + "\r\n\r\n");
                        responsePrintWriter.print(reString);
                        responsePrintWriter.flush();
                    }
                    else if (!requestHeadInfos[0].equalsIgnoreCase("get")) {
                    	//不支持的方法返回501代码
                    	reString="Unimplemented Method:"+requestHeadInfos[0];
                        responsePrintWriter.print("HTTP/1.0 501 Not Implemented\r\n");
                        responsePrintWriter.print("Content-Length: " + reString.length() + "\r\n\r\n");
                        responsePrintWriter.print(reString);
                        responsePrintWriter.flush();
                    }
                    else {
                    	//合法请求时，对请求处理
                        fileInputStream = null;
                        try {
                        	//尝试读取请求文件
                            requestFile = new File("." + requestHeadInfos[1]);
                            fileInputStream = new FileInputStream(requestFile);
                           
                        }
                        catch (FileNotFoundException ex) {
                        	//未找到请求文件，回复404
                        	reString="File :"+requestHeadInfos[1].substring(1)+" not Found";
                            responsePrintWriter.print("HTTP/1.0 404 Not Found\r\n");
                            responsePrintWriter.print("Content-Length: " + reString.length() + "\r\n\r\n");
                            responsePrintWriter.print(reString);
                            responsePrintWriter.flush();
                        }
                        if (fileInputStream != null) {
                            responsePrintWriter.print("HTTP/1.0 ");
                            
                            responsePrintWriter.print("200 OK\r\n");
                            
                            
                            
                            
                            //发送文件内容
                            responsePrintWriter.print("Content-Length: " + requestFile.length() + "\r\n\r\n");
                            responsePrintWriter.flush();
                            final byte[] fileDataPart = new byte[1024];
                            int realGetDataNum;
                            for (int i = (int)requestFile.length(); i > 0; i -= realGetDataNum) {
                            	//i剩余文件内容长度小于1024则直接读取i长度的内容并发送，否则读取1024长度内容并发送
                                if (i <= 1024) {
                                    realGetDataNum = fileInputStream.read(fileDataPart, 0, i);
                                    responseDataOutputStream.write(fileDataPart, 0, realGetDataNum);
                                }
                                else {
                                    realGetDataNum = fileInputStream.read(fileDataPart, 0, 1024);
                                    responseDataOutputStream.write(fileDataPart, 0, realGetDataNum);
                                }
                                
                                responseDataOutputStream.flush();
                            }
                        }
                    }
                }
                //断开连接
                if (fileInputStream != null) {
                    fileInputStream.close();
                    fileInputStream = null;
                }
                if (requestBufferedReader != null) {
                    requestBufferedReader.close();
                    requestBufferedReader = null;
                }
                if (responsePrintWriter != null) {
                    responsePrintWriter.close();
                    responsePrintWriter = null;
                }
                if (requestSocket != null) {
                    requestSocket.close();
                    requestSocket = null;
                }
               
                connectionCount++;
            }
            catch (IOException ex) {
                System.err.println("Request I/O error: " + ex);
                System.out.println("Maybe File is too large.");
                System.exit(1);
            }
        }
    }
}

