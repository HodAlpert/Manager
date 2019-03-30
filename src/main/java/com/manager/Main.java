package com.manager;

import com.manager.Threads.ClientsListener;
import com.manager.Threads.WorkerListener;
import com.manager.common.init;

public class Main {
    public static void main(String[] args)
    {
        init.main();
        init.executor.submit(new ClientsListener());
        init.executor.submit(new WorkerListener());
    }

}