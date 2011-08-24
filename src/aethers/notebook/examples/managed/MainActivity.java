package aethers.notebook.examples.managed;

import aethers.notebook.core.AethersNotebook;
import aethers.notebook.examples.R;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;

public class MainActivity
extends Activity
{
    private final ServiceConnection loggerConnection = new ServiceConnection()
    {
        @Override
        public void onServiceDisconnected(ComponentName name) 
        {
            aethersNotebook = null;
            loggerButton.setEnabled(false);
            loggerButton.setText("Waiting for connection...");
            appenderButton.setEnabled(false);
            appenderButton.setText("Waiting for connection...");
        }
        
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) 
        {
            aethersNotebook = AethersNotebook.Stub.asInterface(service);
            loggerButton.setEnabled(true);
            try
            {
                loggerButton.setText(
                        aethersNotebook.isManagedLoggerInstalled(ExampleManagedLoggerService.IDENTIFIER) 
                                ? "Uninstall"
                                : "Install");
            }
            catch(RemoteException e)
            {
                throw new RuntimeException(e);
            }
            appenderButton.setEnabled(true);
            try
            {
                appenderButton.setText(
                        aethersNotebook.isManagedAppenderInstalled(ExampleManagedAppenderService.IDENTIFIER) 
                                ? "Uninstall"
                                : "Install");
            }
            catch(RemoteException e)
            {
                throw new RuntimeException(e);
            }
        }
    };
    
    private AethersNotebook aethersNotebook;
    
    private Button loggerButton;
    
    private Button appenderButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.managedmain);
    }

    @Override
    protected void onResume() 
    {
        super.onResume();
        loggerButton = (Button)findViewById(R.id.managed_logger_button);
        appenderButton = (Button)findViewById(R.id.managed_appender_button);
        Intent i = new Intent("aethers.notebook.action.ACTION_CONNECT");
        bindService(i, loggerConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() 
    {
        super.onPause();
        unbindService(loggerConnection);
        aethersNotebook = null;
    }
    
    public void loggerButtonAction(View v)
    {
        try
        {
            if(aethersNotebook.isManagedLoggerInstalled(ExampleManagedLoggerService.IDENTIFIER))
            {
                aethersNotebook.deregisterManagedLogger(ExampleManagedLoggerService.IDENTIFIER);
                loggerButton.setText("Install");
            }
            else
            {
                aethersNotebook.registerManagedLogger(ExampleManagedLoggerService.IDENTIFIER);
                loggerButton.setText("Uninstall");
            }
        }
        catch(RemoteException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    public void appenderButtonAction(View v)
    {
        try
        {
            if(aethersNotebook.isManagedAppenderInstalled(ExampleManagedAppenderService.IDENTIFIER))
            {
                aethersNotebook.deregisterManagedAppender(ExampleManagedAppenderService.IDENTIFIER);
                appenderButton.setText("Install");
            }
            else
            {
                aethersNotebook.registerManagedAppender(ExampleManagedAppenderService.IDENTIFIER);
                appenderButton.setText("Uninstall");
            }
        }
        catch(RemoteException e)
        {
            throw new RuntimeException(e);
        }
    }
}
