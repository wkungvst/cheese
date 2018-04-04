package interfaces;

import java.util.TreeMap;

/**
 * Created by wkung on 12/12/17.
 */

public interface ICatalogInterface {
    void populateList(TreeMap<Integer,Integer> yearData);
    void onBackPressed();

     void setCurrentYear();

}
