package org.multiverse.stms.beta.transactionalobjects;

public class Log {
    private String[] array = new String[100];
    private int index = 0;

    public synchronized void log(String log) {
        array[index] = log;
        index++;
        if (index == array.length) {
            index = 0;
        }
    }

    public synchronized void print(){
        System.out.println("--------------------------------------");
        for(int k=0;k<array.length;k++){
            if(k == index -1){
                System.out.println("* "+array[k]);
            }else{
                System.out.println("  "+array[k]);
            }
        }
    }
}
