package aethers.notebook.examples.managed;

import aethers.notebook.core.AethersNotebook;
import aethers.notebook.core.LoggerService;
import aethers.notebook.core.LoggerServiceIdentifier;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

public class ExampleManagedLoggerService
extends Service
implements Runnable
{
    public static final LoggerServiceIdentifier IDENTIFIER = new LoggerServiceIdentifier(
            "aethers.notebook.examples.managed.ExampleManagedLoggerService");
    static 
    {
        IDENTIFIER.setConfigurable(false);
        IDENTIFIER.setDescription("Periodically sends a message");
        IDENTIFIER.setName("Example Managed Logger");
        IDENTIFIER.setPackageName("aethers.notebook.examples");
        IDENTIFIER.setServiceClass(ExampleManagedLoggerService.class.getName());
        IDENTIFIER.setVersion(1);
    }

    private static final String ENCODING = "UTF-8";
    
    private final LoggerService.Stub loggerServiceStub = new LoggerService.Stub()
    {  
        @Override
        public void stop() 
        throws RemoteException 
        {
            ExampleManagedLoggerService.this.stopSelf();
        }
        
        @Override
        public void start()
        throws RemoteException 
        {
            startService(new Intent(
                    ExampleManagedLoggerService.this, 
                    ExampleManagedLoggerService.this.getClass()));
        }
        
        @Override
        public boolean isRunning()
        throws RemoteException 
        {
            synchronized(sync)
            {
                return running;
            }
        }
        
        @Override
        public void configure() 
        throws RemoteException { }
    };
    
    private final ServiceConnection loggerConnection = 
            new ServiceConnection() 
            {   
                @Override
                public void onServiceDisconnected(ComponentName name)
                {
                    ExampleManagedLoggerService.this.stopSelf();
                }
                
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) 
                {
                    aethersNotebook = AethersNotebook.Stub.asInterface(service);
                    new Thread(ExampleManagedLoggerService.this).start();
                }
            };

    @Override
    public IBinder onBind(Intent intent) 
    {
        return loggerServiceStub;
    }
    
    private final Object sync = new Object();
    
    protected AethersNotebook aethersNotebook;
    
    private volatile boolean running = false;
    
    private Thread thread;
    
    @Override
    public void onDestroy() 
    {
        super.onDestroy();
        synchronized(sync)
        {
            if(running)
            {
                running = false;
                unbindService(loggerConnection);
                if(thread != null)
                {
                    thread.interrupt();
                    thread = null;
                }
            }
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) 
    {
        synchronized(sync)
        {
            if(running)
                return START_STICKY;
            running = bindService(
                    new Intent("aethers.notebook.action.ACTION_CONNECT"),
                    loggerConnection, 
                    BIND_AUTO_CREATE);
        }
        
        return START_STICKY;
    }
    
    public void run()
    {
        synchronized(sync)
        {
            thread = Thread.currentThread();
            while(running)
            {
                try
                {
                    aethersNotebook.log(IDENTIFIER, 
                            "Message from managed logger".getBytes(ENCODING));
                    sync.wait(5000);
                }
                catch(Exception e) { }
            }
        }
    }
}
