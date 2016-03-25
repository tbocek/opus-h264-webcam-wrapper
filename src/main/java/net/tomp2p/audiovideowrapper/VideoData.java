/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.tomp2p.audiovideowrapper;

import java.nio.ByteBuffer;

/**
 *
 * @author draft
 */
public interface VideoData {

    public void created(ByteBuffer buffer, long time, int w, int h);
    
}
