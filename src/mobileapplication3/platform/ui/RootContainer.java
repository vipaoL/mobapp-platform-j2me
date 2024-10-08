/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mobileapplication3.platform.ui;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;

import mobileapplication3.ui.IContainer;
import mobileapplication3.ui.IUIComponent;
import mobileapplication3.ui.UISettings;

/**
 *
 * @author vipaol
 */
public class RootContainer extends GameCanvas implements IContainer {
    
    private IUIComponent rootUIComponent = null;
    private KeyboardHelper kbHelper;
    public static boolean displayKbHints = false;
    private int bgColor = 0x000000;
    public int w, h;
    private static RootContainer inst = null;
    private UISettings uiSettings;
    private boolean wasDownEvent = false;

    public RootContainer(IUIComponent rootUIComponent, UISettings uiSettings) {
    	super(false);
        setFullScreenMode(true);
        this.uiSettings = uiSettings;
        inst = this;
        kbHelper = new KeyboardHelper();
        displayKbHints = !hasPointerEvents();
        setRootUIComponent(rootUIComponent);
    }

    public void init() {
    	if (rootUIComponent != null) {
    		rootUIComponent.init();
    	}
	}

    public static RootContainer setRootUIComponent(IUIComponent rootUIComponent) {
    	inst.wasDownEvent = false;
        if (inst.rootUIComponent != null) {
            inst.rootUIComponent.setParent(null);
            inst.rootUIComponent.setFocused(false);
        }
        
        if (rootUIComponent != null) {
		    inst.rootUIComponent = rootUIComponent.setParent(inst).setFocused(true);
		    rootUIComponent.setSize(inst.getWidth(), inst.getHeight());
		    rootUIComponent.init();
		    rootUIComponent.setFocused(true);
        }
        return inst;
    }
    
    public UISettings getUISettings() {
		return uiSettings;
	}

    public final void repaintt() {
    	if (rootUIComponent == null || !rootUIComponent.repaintOnlyOnFlushGraphics()) {
    		super.repaint();
    	}
    }

    public void paint(Graphics g) {
    	if (bgColor >= 0) {
    		g.fillRect(0, 0, w, h);
    	}

        if (rootUIComponent != null) {
            rootUIComponent.paint(new mobileapplication3.platform.ui.Graphics(g));
        } else {
        	g.setColor(0xaaaaaa);
        	g.drawString("Nothing to draw. " + rootUIComponent, w/2, h, Graphics.BOTTOM | Graphics.HCENTER);
        }
    }
    
    public mobileapplication3.platform.ui.Graphics getUGraphics() {
		return new mobileapplication3.platform.ui.Graphics(getGraphics());
	}
    
    public int getBgColor() {
		return bgColor;
	}
    
    public void setBgColor(int bgColor) {
		this.bgColor = bgColor;
	}
    
    public static int getGameActionn(int keyCode) {
    	return inst.getGameAction(keyCode);
    }
    
    protected void keyPressed(int keyCode) {
        kbHelper.keyPressed(keyCode);
    }
    
    private void handleKeyPressed(int keyCode, int count) {
    	wasDownEvent = true;
        if (rootUIComponent != null) {
            rootUIComponent.setVisible(true);
            if (rootUIComponent.keyPressed(keyCode, count)) {
                repaintt();
            }
        }
    }

    protected void keyReleased(int keyCode) {
        kbHelper.keyReleased(keyCode);
    }
    
    private void handleKeyReleased(int keyCode, int count) {
        if (rootUIComponent != null && wasDownEvent) {
            rootUIComponent.setVisible(true);
            if (rootUIComponent.keyReleased(keyCode, count)) {
                repaintt();
            }
        }
    }
    
    protected void handleKeyRepeated(int keyCode, int pressedCount) {
        if (getGameAction(keyCode) == Canvas.FIRE) {
            return;
        }
        if (rootUIComponent != null && wasDownEvent) {
            if (rootUIComponent.keyRepeated(keyCode, pressedCount)) {
                repaintt();
            }
        }
    }
    
    protected void pointerPressed(int x, int y) {
    	wasDownEvent = true;
        if (rootUIComponent != null) {
            rootUIComponent.setVisible(true);
            if (rootUIComponent.pointerPressed(x, y)) {
                repaintt();
            }
        }
    }
    
    protected void pointerDragged(int x, int y) {
        if (rootUIComponent != null && wasDownEvent) {
            if (rootUIComponent.pointerDragged(x, y)) {
                repaintt();
            }
        }
    }
    
    protected void pointerReleased(int x, int y) {
        if (rootUIComponent != null && wasDownEvent) {
            if (rootUIComponent.pointerReleased(x, y)) {
                repaintt();
            }
        }
    }
    
    protected void sizeChanged(int w, int h) {
    	this.w = w;
    	this.h = h;
        if (rootUIComponent != null) {
            rootUIComponent.setSize(w, h);
            repaintt();
        }
    }

    protected void showNotify() {
        kbHelper.show();
        if (rootUIComponent != null) {
            rootUIComponent.setVisible(true);
            repaintt();
        }
        sizeChanged(getWidth(), getHeight());
    }
    
    protected void hideNotify() {
        kbHelper.hide();
        if (rootUIComponent != null) {
            rootUIComponent.setVisible(false);
            repaintt();
        }
    }
    
    private class KeyboardHelper {
        private Object tillPressed = new Object();
        private int lastKey, pressCount;
        private boolean pressState;
        private Thread repeatThread;
        private long lastEvent;

        public void show() {
            pressState = false;
            pressCount = 1;
            lastKey = 0;
            repeatThread = new Thread() {
                public void run() {
                    try {
                        while(true) {
                            if(!pressState) {
                                synchronized(tillPressed) {
                                    tillPressed.wait();
                                }
                            }
                            
                            int k = lastKey;
                            Thread.sleep(200);
                            while (!isLastEventOld()) {
                                Thread.sleep(200);
                            }
                            
                            while(pressState && lastKey == k) {
                                handleKeyRepeated(k, pressCount);
                                Thread.sleep(100);
                            }
                            
                            pressCount = 1;
                        }
                    } catch (InterruptedException e) { }
                }
            };
            repeatThread.start();
        }
        
        public void hide() {
            if(repeatThread != null) {
                repeatThread.interrupt();
            }
        }

        public void keyPressed(int k) {
            if (!isLastEventOld() && k == lastKey) {
                pressCount++;
            } else {
                pressCount = 1;
            }
            
            updateLastEventTime();
            lastKey = k;
            pressState = true;
            synchronized(tillPressed) {
                tillPressed.notify();
            }
            handleKeyPressed(k, pressCount);
        }

        public void keyReleased(int k) {
            updateLastEventTime();
            if(lastKey == k) {
                pressState = false;
            } else {
                pressCount = 0;
            }
            handleKeyReleased(k, pressCount);
        }
        
        private boolean isLastEventOld() {
            return System.currentTimeMillis() - lastEvent > 200;
        }
        
        private void updateLastEventTime() {
            lastEvent = System.currentTimeMillis();
        }
    }
}
