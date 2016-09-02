package net.ericsonj.serial_monitor;

import java.util.Stack;

/**
 * Stack Buffer
 *
 * @author ejoseph
 */
public class StackBuffer {

    /**
     * @param args the command line arguments
     */
    private final Stack<String> sBuffer;
    private final int size;
    private static int pointer;

    public StackBuffer(int size) {
        this.size = size;
        this.sBuffer = new Stack<>();
        this.pointer = 0;
    }

    public void push(String command) {
        isMaxSize();
        if (!sBuffer.empty()) {
            if(!sBuffer.peek().equals(command)){
                sBuffer.push(command);
            }
        }else{
            sBuffer.push(command);
        } 
        this.pointer = -1;
    }

    private void isMaxSize() {
        if (sBuffer.size() == size) {
            sBuffer.remove(0);
        }
    }

    public String getUP() {
        if(sBuffer.size() == 0){
            return "";
        }
        int index = sBuffer.size() - 1;

        index -= (pointer + 1);
        if (index >= 0) {
            pointer++;
        }
        if (index > 0) {
            return  sBuffer.elementAt(index);
        } else {
            return  sBuffer.firstElement();
        }
    }

    public String getDOWN() {
        if(sBuffer.size() == 0){
            return "";
        }
        int index = sBuffer.size() - 1;
        index = index - (pointer-1);
        if (pointer > -1) {
            pointer--;
        }
        if (pointer >= -1 && index < (sBuffer.size())) {
            return  sBuffer.elementAt(index);
        } else {
            return "";
        }
    }
}
