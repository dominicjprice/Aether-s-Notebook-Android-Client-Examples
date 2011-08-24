package aethers.notebook.examples.unmanaged;

import java.io.UnsupportedEncodingException;

import aethers.notebook.core.AethersNotebook;
import aethers.notebook.core.AppenderServiceIdentifier;
import aethers.notebook.core.LoggerServiceIdentifier;
import aethers.notebook.core.TimeStamp;
import aethers.notebook.core.UnmanagedAppenderService;
import aethers.notebook.examples.R;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity 
extends Activity
{
    private static final AppenderServiceIdentifier APPENDER_IDENTIFIER = 
            new AppenderServiceIdentifier("aethers.notebook.examples.unmanaged.MainActivity");
    static
    {
        APPENDER_IDENTIFIER.setConfigurable(false);
        APPENDER_IDENTIFIER.setDescription("Example unmanaged appender");
        APPENDER_IDENTIFIER.setName("Unmanaged Appender Example");
        APPENDER_IDENTIFIER.setVersion(1);
    }
    
    private static final LoggerServiceIdentifier LOGGER_IDENTIFIER = 
            new LoggerServiceIdentifier("aethers.notebook.examples.unmanaged.MainActivity");
    static
    {
        LOGGER_IDENTIFIER.setConfigurable(false);
        LOGGER_IDENTIFIER.setDescription("Example unmanaged logger");
        LOGGER_IDENTIFIER.setName("Unmanaged Logger Example");
        LOGGER_IDENTIFIER.setVersion(1);
    }
    
    private final UnmanagedAppenderService appender = new UnmanagedAppenderService.Stub()
    {
        @Override
        public void log(
                LoggerServiceIdentifier identifier,
                TimeStamp timestamp,
                Location location,
                byte[] data)
        throws RemoteException 
        {
            appendMessage(identifier, timestamp, location, data);
        }
    };
    
    private final ServiceConnection loggerConnection = new ServiceConnection()
    {
        @Override
        public void onServiceDisconnected(ComponentName name) 
        {
            connected = false;
            aethersNotebook = null;
            connectionStatus.setChecked(false);
            try
            {
                aethersNotebook.deregisterUnmanagedAppender(APPENDER_IDENTIFIER);
            }
            catch(RemoteException e)
            {
                Toast.makeText(
                        MainActivity.this, 
                        "An error occurred during appender deregistration",
                        Toast.LENGTH_LONG);
            }
        }
        
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) 
        {
            aethersNotebook = AethersNotebook.Stub.asInterface(service);
            connected = true;
            connectionStatus.setChecked(true);
            try
            {
                aethersNotebook.registerUnmanagedAppender(APPENDER_IDENTIFIER, appender);
            }
            catch(RemoteException e)
            {
                Toast.makeText(
                        MainActivity.this, 
                        "An error occurred during appender registration",
                        Toast.LENGTH_LONG);
            }
        }
    };
    
    private AethersNotebook aethersNotebook;
    
    private boolean connected = false;
    
    private CheckBox connectionStatus;
    
    private ListView messageList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unmanagedmain);
        connectionStatus = (CheckBox)findViewById(R.id.unmanaged_connected);
        messageList = (ListView)findViewById(R.id.message_list);
        messageList.setAdapter(new ArrayAdapter<String>(this, R.layout.logentry));
    }

    @Override
    protected void onResume() 
    {
        super.onResume();
        Intent i = new Intent("aethers.notebook.action.ACTION_CONNECT");
        bindService(i, loggerConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() 
    {
        super.onPause();
        unbindService(loggerConnection);
        aethersNotebook = null;
        connected = false;
        connectionStatus.setChecked(false);
    }    
    
    public void sendLogMessage(View v)
    {
        if(!connected)
            return;
        try
        {
            EditText et = (EditText)findViewById(R.id.unmanaged_message);
            aethersNotebook.log(LOGGER_IDENTIFIER, et.getText().toString().getBytes("UTF-8"));
            et.setText("");
        }
        catch(Exception e)
        {
            Toast.makeText(this, "An error occurred during logging.", Toast.LENGTH_LONG);
        }
    }
    
    @SuppressWarnings("unchecked")
    private synchronized void appendMessage(
            LoggerServiceIdentifier identifier,
            TimeStamp timestamp,
            Location location,
            byte[] data)
    {
        try
        {
            String msg = identifier.getUniqueID().equals(APPENDER_IDENTIFIER.getUniqueID())
                    ? new String(data, "UTF-8")
                    : "<Data from '" + identifier.getName() + "'>";
            ArrayAdapter<String> a = (ArrayAdapter<String>)messageList.getAdapter();
            a.insert(msg, 0);
            while(a.getCount() > 100)
                a.remove(a.getItem(100));
                
        }
        catch(UnsupportedEncodingException e)
        {
        
        }
    }
}
