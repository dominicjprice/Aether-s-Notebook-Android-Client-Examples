package aethers.notebook.examples.managed;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import aethers.notebook.core.Action;
import aethers.notebook.core.AppenderServiceIdentifier;
import aethers.notebook.core.LoggerServiceIdentifier;
import aethers.notebook.core.ManagedAppenderService;
import aethers.notebook.core.TimeStamp;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.widget.Toast;

public class ExampleManagedAppenderService
extends Service
implements Runnable
{
    public static final AppenderServiceIdentifier IDENTIFIER = new AppenderServiceIdentifier(
            "aethers.notebook.examples.managed.ExampleManagedAppenderService");
    static 
    {
        IDENTIFIER.setConfigurable(false);
        IDENTIFIER.setDescription("Displays messages from the example logger");
        IDENTIFIER.setName("Example Managed Appender");
        IDENTIFIER.setPackageName("aethers.notebook.examples");
        IDENTIFIER.setServiceClass(ExampleManagedAppenderService.class.getName());
        IDENTIFIER.setVersion(1);
    }
    
    private static final String ENCODING = "UTF-8";
    
    private final ManagedAppenderService.Stub appenderServiceStub = new ManagedAppenderService.Stub()
    {  
        @Override
        public void stop() 
        throws RemoteException 
        {
            ExampleManagedAppenderService.this.stopSelf();
        }
        
        @Override
        public void start()
        throws RemoteException 
        {
            startService(new Intent(
                    ExampleManagedAppenderService.this,
                    ExampleManagedAppenderService.this.getClass()));
        }
        
        @Override
        public void log(
                LoggerServiceIdentifier identifier,
                TimeStamp timestamp,
                Location location,
                final byte[] data) 
        throws RemoteException 
        {
            if(!identifier.equals(ExampleManagedLoggerService.IDENTIFIER))
                return;
            handler.post(new Runnable()
            {
                @Override
                public void run() 
                {
                    try
                    {
                        Toast.makeText(
                                ExampleManagedAppenderService.this,
                                new String(data, ENCODING),
                                Toast.LENGTH_SHORT).show();
                    }
                    catch(UnsupportedEncodingException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            });
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

        @Override
        public List<Action> listActions() 
        throws RemoteException
        {
            return new ArrayList<Action>();
        }

        @Override
        public void doAction(Action action) 
        throws RemoteException { }
    };
    
    private final Object sync = new Object();
    
    private volatile boolean running = false;
    
    private Handler handler;
    
    @Override
    public IBinder onBind(Intent intent) 
    {
        return appenderServiceStub;
    }
    
    @Override
    public void onDestroy() 
    {
        super.onDestroy();
        synchronized(sync)
        {
            if(running)
            {
                running = false;
                if(handler != null)
                {
                    handler.getLooper().quit();
                    handler = null;
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
            running = true;
            new Thread(this).start();
            return START_STICKY;
        }        
    }

    @Override
    public void run() 
    {
        Looper.prepare();
        handler = new Handler();        
        Looper.loop();
    }
}
