/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.core.network.rcon;

import net.minecraft.network.rcon.IServer;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.network.rcon.RConThreadBase;
import net.minecraft.network.rcon.RConThreadClient;
import net.minecraft.network.rcon.RConUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.source.RconSource;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.network.rcon.RconConnectionEvent;
import org.spongepowered.api.network.RemoteConnection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.interfaces.IMixinRConConsoleSource;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutionException;

@Mixin(RConThreadClient.class)
public abstract class MixinRConThreadClient extends RConThreadBase implements RemoteConnection {

    private static final Logger LOGGER = LogManager.getLogger();

    @Shadow private void sendMultipacketResponse(int p_72655_1_, String p_72655_2_) throws IOException {}
    @Shadow private void sendResponse(int p_72654_1_, int p_72654_2_, String message) throws IOException {}
    @Shadow private void closeSocket() {}
    @Shadow private void sendLoginFailedResponse() throws IOException {}

    @Shadow private boolean loggedIn;
    @Shadow private Socket clientSocket;
    @Shadow @Final private String rconPassword;
    @Shadow @Final private byte[] buffer;

    private RConConsoleSource source;

    protected MixinRConThreadClient(IServer p_i45300_1_, String p_i45300_2_) {
        super(p_i45300_1_, p_i45300_2_);
    }

    @Override
    public InetSocketAddress getAddress() {
        return (InetSocketAddress) this.clientSocket.getRemoteSocketAddress();
    }

    @Override
    public InetSocketAddress getVirtualHost() {
        return (InetSocketAddress) this.clientSocket.getLocalSocketAddress();
    }

    @Inject(method = "closeSocket", at = @At("HEAD"))
    public void rconLogoutCallback(CallbackInfo ci) {
        if (this.loggedIn) {
            SpongeImpl.getScheduler().callSync(() -> {
                final CauseStackManager causeStackManager = Sponge.getCauseStackManager();
                causeStackManager.pushCause(this);
                causeStackManager.pushCause(this.source);
                final RconConnectionEvent.Disconnect event = SpongeEventFactory.createRconConnectionEventDisconnect(
                        causeStackManager.getCurrentCause(), (RconSource) this.source);
                SpongeImpl.postEvent(event);
                causeStackManager.popCauses(2);
                return event;
            });
        }
    }

    /**
     * @author Cybermaxke
     * @reason Fix RCON, is completely broken
     */
    @Override
    @Overwrite
    public void run() {
        /// Sponge: START
        // Initialize the source
        this.source = new RConConsoleSource(SpongeImpl.getServer());
        ((IMixinRConConsoleSource) this.source).setConnection((RConThreadClient) (Object) this);

        // Call the connection event
        final RconConnectionEvent.Connect connectEvent;
        try {
            connectEvent = SpongeImpl.getScheduler().callSync(() -> {
                final CauseStackManager causeStackManager = Sponge.getCauseStackManager();
                causeStackManager.pushCause(this);
                causeStackManager.pushCause(this.source);
                final RconConnectionEvent.Connect event = SpongeEventFactory.createRconConnectionEventConnect(
                        causeStackManager.getCurrentCause(), (RconSource) this.source);
                SpongeImpl.postEvent(event);
                causeStackManager.popCauses(2);
                return event;
            }).get();
        } catch (InterruptedException | ExecutionException ignored) {
            closeSocket();
            return;
        }
        if (connectEvent.isCancelled()) {
            closeSocket();
            return;
        }
        /// Sponge: END
        /// Sponge: All 'return' operations are replaced by 'break' and
        ///         'closeSocket' is moved out of the loop
        while (true) {
            try {
                if (!this.running) {
                    break;
                }

                final BufferedInputStream bufferedinputstream = new BufferedInputStream(this.clientSocket.getInputStream());
                final int i = bufferedinputstream.read(this.buffer, 0, this.buffer.length);
                /// Sponge: START
                if (i == -1) {
                    // We read end of file, which means that this socket should be closed.
                    // Break to exit the loop. As the read didn't advance anything, it will
                    // continue to return EOF until we manually exit the loop.
                    break;
                }
                /// Sponge: END

                if (10 <= i) {
                    int j = 0;
                    final int k = RConUtils.getBytesAsLEInt(this.buffer, 0, i);

                    if (k != i - 4) {
                        break;
                    }

                    j += 4;
                    final int l = RConUtils.getBytesAsLEInt(this.buffer, j, i);
                    j += 4;
                    final int i1 = RConUtils.getRemainingBytesAsLEInt(this.buffer, j);
                    j += 4;

                    switch (i1) {
                        case 2:
                            if (this.loggedIn) {
                                final String command = RConUtils.getBytesAsString(this.buffer, j, i);
                                try {
                                    /// Sponge: START
                                    // Execute the command on the main thread and wait for it
                                    SpongeImpl.getScheduler().callSync(() -> {
                                        final CauseStackManager causeStackManager = Sponge.getCauseStackManager();
                                        // Only add the RemoteConnection here, the RconSource
                                        // will be added by the command manager
                                        causeStackManager.pushCause(this);
                                        SpongeImpl.getServer().getCommandManager().executeCommand(this.source, command);
                                        causeStackManager.popCause();
                                    }).get();
                                    final String logContents = this.source.getLogContents();
                                    this.source.resetLog();
                                    sendMultipacketResponse(l, logContents);
                                    /// Sponge: END
                                } catch (Exception exception) {
                                    sendMultipacketResponse(l, "Error executing: " + command + " (" + exception.getMessage() + ")");
                                }
                                continue;
                            }
                            sendLoginFailedResponse();
                            break; // Sponge: 'continue' -> 'break', disconnect when a invalid operation is requested
                        case 3:
                            final String password = RConUtils.getBytesAsString(this.buffer, j, i);
                            if (!password.isEmpty() && password.equals(this.rconPassword)) {
                                /// Sponge: START
                                final RconConnectionEvent.Login event = SpongeImpl.getScheduler().callSync(() -> {
                                    final CauseStackManager causeStackManager = Sponge.getCauseStackManager();
                                    causeStackManager.pushCause(this);
                                    causeStackManager.pushCause(this.source);
                                    final RconConnectionEvent.Login event1 = SpongeEventFactory.createRconConnectionEventLogin(
                                            causeStackManager.getCurrentCause(), (RconSource) this.source);
                                    SpongeImpl.postEvent(event1);
                                    causeStackManager.popCauses(2);
                                    return event1;
                                }).get();
                                if (!event.isCancelled()) {
                                    this.loggedIn = true;
                                    sendResponse(l, 2, "");
                                    continue;
                                }
                                /// Sponge: END
                            }

                            this.loggedIn = false;
                            sendLoginFailedResponse();
                            break; // Sponge: 'continue' -> 'break', disconnect if login failed
                        default:
                            sendMultipacketResponse(l, String.format("Unknown request %s", Integer.toHexString(i1)));
                            break; // Sponge: 'continue' -> 'break', disconnect when a invalid operation is requested
                    }
                }
            } catch (IOException e) {
                break;
            } catch (Exception e) {
                LOGGER.error("Exception whilst parsing RCON input", e);
                break;
            }
        }
        closeSocket();
    }
}
