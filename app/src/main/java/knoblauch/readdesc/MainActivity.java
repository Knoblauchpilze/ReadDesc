package knoblauch.readdesc;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the adapter to display recent reads.
        ReadsAdapter adapter = new ReadsAdapter(this, 10);

        // Use this adapter as the content for the recent reads view.
        ListView recentReads = findViewById(R.id.recent_reads_list);
        recentReads.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Call base handler.
        boolean ret = super.onCreateOptionsMenu(menu);

        // Inflate the options menu as described in the corresponding resource.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.read_actions_menu, menu);

        // TODO: Implement this.

        return ret;
    }
}
