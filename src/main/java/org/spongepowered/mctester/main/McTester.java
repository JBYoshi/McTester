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
package org.spongepowered.mctester.main;

import com.google.inject.Inject;
import org.spongepowered.mctester.main.framework.proxy.RemoteInvocationData;
import org.spongepowered.mctester.main.framework.proxy.RemoteInvocationDataBuilder;
import org.spongepowered.mctester.main.message.ClientDelegateHandler;
import org.spongepowered.mctester.main.message.ServerDelegateHandler;
import org.spongepowered.mctester.main.message.toclient.MessageRPCRequest;
import org.slf4j.Logger;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.network.ChannelBinding;
import org.spongepowered.api.network.MessageHandler;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.mctester.main.message.toserver.MessageRPCResponse;

import java.util.Collection;

@Plugin(
        id = "mctester",
        name = "McTester",
        description = "A framework for writing fully automated tests of Minecraft",
        authors = {
                "Aaron1011"
        }
)
public class McTester {

    public volatile static McTester INSTANCE;

    @Inject
    public volatile Logger logger;

    public SpongeExecutorService syncExecutor;

    public volatile ChannelBinding.IndexedMessageChannel channel;

    public McTester() {
        INSTANCE = this;
    }


    @Listener
    public void onInit(GameInitializationEvent event) {
        this.syncExecutor = Sponge.getScheduler().createSyncExecutor(this);
        this.channel = Sponge.getChannelRegistrar().createChannel(this, "mctester");

        ClientDelegateHandler clientDelegateHandler = new ClientDelegateHandler();
        ServerDelegateHandler serverDelegateHandler = new ServerDelegateHandler();

        this.channel.registerMessage(MessageRPCRequest.class, 0, Platform.Type.CLIENT, (MessageHandler) clientDelegateHandler);
        this.channel.registerMessage(MessageRPCResponse.class, 1, Platform.Type.SERVER, (MessageHandler) serverDelegateHandler);

        if (Sponge.getPlatform().getExecutionType().equals(Platform.Type.CLIENT)) {
            Sponge.getDataManager().registerBuilder(RemoteInvocationData.class, new RemoteInvocationDataBuilder(ClientOnly.REAL_CLIENT_HANDLER));
        }

        /*Sponge.getCommandManager().register(this, CommandSpec.builder().executor(new CommandExecutor() {

                    @Override
                    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                        TesterManager.runTestThread();
                        src.sendMessage(Text.of(TextColors.GREEN, "Here we go!"));

                        return CommandResult.success();
                    }
                }).build(),
                "runTest");*/
    }

    public static Player getThePlayer() {
        Collection<Player> players = Sponge.getServer().getOnlinePlayers();
        if (players.size() != 1) {
            throw new RuntimeException("Unexpected players: " + players);
        }
        return players.iterator().next();
    }

    public void sendToPlayer(MessageRPCRequest messageRPCRequest) {
        this.channel.sendTo(McTester.getThePlayer(), messageRPCRequest);
    }
}