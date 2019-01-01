package com.NIO;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class MultiplexerTimeServer implements Runnable {

    private Selector selector;

    private ServerSocketChannel serverChannel;

    private volatile boolean stop;

    /*
     * 初始化多路复用器，绑定监听端口，队列长度
     * @param port
     * */
    public MultiplexerTimeServer(int port) {
        try {
            selector = Selector.open();                              //打开复路器
            serverChannel = ServerSocketChannel.open();             //打开通道，监听客户端连接
            serverChannel.configureBlocking(false);                //设置连接为非阻塞模式
            serverChannel.socket().bind(new InetSocketAddress(port), 1024); //绑定监听端口，和请求队列的长度
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);              //将通道注册到复路器上Seletor上，监听ACCEPT事件
            System.out.println("the time server is start in port:" + port);
        } catch (Exception e) {
            e.printStackTrace();
            //初始化失败，退出
            System.exit(1);
        }
    }

    public void stop() {
        this.stop = true;
    }

    /*
     * 多路复用器在线程run方法的无限循环体内轮询准备就绪的key
     * */
    @Override
    public void run() {
        while (!stop) {
            try {
                selector.select(1000);                            //无论读写，复路器没隔一秒被唤醒一次
                Set<SelectionKey> selectionKeys = selector.selectedKeys(); //当有就绪的channel，selector就返回channel的selectionKey
                Iterator<SelectionKey> it = selectionKeys.iterator();
                SelectionKey key = null;
                while (it.hasNext()) {      //根据selectionKey的操作位判断网络类型
                    key = it.next();
                    it.remove();
                    try {
                        handleInput(key);     //上述相当于完成TCP的三次握手，TCP物理链路正式建立
                    } catch (Exception e1) {
                        if (key != null) {
                            key.cancel();
                            if (key.channel() != null) {
                                key.channel().close();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleInput(SelectionKey key) throws Exception {
        if (key.isValid()) {
            //处理新接入的请求消息
            if (key.isAcceptable()) {
                //Accept the new connection
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();   //
                SocketChannel sc = ssc.accept();
                sc.register(selector, SelectionKey.OP_READ);
            }
            if (key.isReadable()) {
                //read the data
                SocketChannel sc = (SocketChannel) key.channel();     //被选中(复路器轮询)的channel
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);   //无法得知客户端大小，创建1MB缓冲区
                int readBytes = sc.read(readBuffer);                //获取请求码流
                if (readBytes > 0) {
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    String body = new String(bytes, "UTF-8");
                    System.out.println("the time server receive order :" + body);
                    String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
                    doWrite(sc, currentTime);
                } else if (readBytes < 0) {
                    //对端链路关闭
                    key.cancel();
                    sc.close();
                } else {
                    ;//读到 0 字节，忽略
                }
            }
        }
    }


    public void doWrite(SocketChannel channel, String response) throws Exception {
        if (response != null && response.trim().length() > 0) {
            byte[] bytes = response.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            writeBuffer.flip();
            channel.write(writeBuffer);
        }


    }


}
