package views;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import kung.cheeseandfriends.R;

/**
 * Created by wkung on 12/14/17.
 */

public class ShowWidget extends RelativeLayout {

    Context mContext;
    public ShowWidget(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public void init(){
        LayoutInflater.from(mContext).inflate(R.layout.show_widget, this, true);
    }
}
