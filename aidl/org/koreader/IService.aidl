package org.koreader;

interface IService {

    /** status */
    boolean enabled();
 	String status();

 	/** overlay */
 	void resume();
 	void pause();
    void setDim(int level);
    void setDimColor(int color);
    void setWarmth(int level);
    void setWarmthAlpha(float alpha);
}

