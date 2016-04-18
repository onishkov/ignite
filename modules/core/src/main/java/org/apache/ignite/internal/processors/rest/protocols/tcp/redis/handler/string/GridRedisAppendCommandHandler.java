/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.rest.protocols.tcp.redis.handler.string;

import java.nio.ByteBuffer;
import java.util.Collection;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.GridKernalContext;
import org.apache.ignite.internal.processors.rest.GridRestProtocolHandler;
import org.apache.ignite.internal.processors.rest.GridRestResponse;
import org.apache.ignite.internal.processors.rest.protocols.tcp.redis.GridRedisCommand;
import org.apache.ignite.internal.processors.rest.protocols.tcp.redis.GridRedisMessage;
import org.apache.ignite.internal.processors.rest.protocols.tcp.redis.GridRedisProtocolParser;
import org.apache.ignite.internal.processors.rest.protocols.tcp.redis.handler.GridRedisStringCommandHandler;
import org.apache.ignite.internal.processors.rest.protocols.tcp.redis.handler.exception.GridRedisGenericException;
import org.apache.ignite.internal.processors.rest.request.GridRestCacheRequest;
import org.apache.ignite.internal.processors.rest.request.GridRestRequest;
import org.apache.ignite.internal.util.typedef.internal.U;

import static org.apache.ignite.internal.processors.rest.GridRestCommand.CACHE_APPEND;
import static org.apache.ignite.internal.processors.rest.GridRestCommand.CACHE_GET;
import static org.apache.ignite.internal.processors.rest.GridRestCommand.CACHE_PUT;
import static org.apache.ignite.internal.processors.rest.protocols.tcp.redis.GridRedisCommand.APPEND;

/**
 * Redis APPEND command handler.
 */
public class GridRedisAppendCommandHandler extends GridRedisStringCommandHandler {
    /** Supported commands. */
    private static final Collection<GridRedisCommand> SUPPORTED_COMMANDS = U.sealList(
        APPEND
    );

    private static final int VAL_POS = 2;

    /** {@inheritDoc} */
    public GridRedisAppendCommandHandler(final GridKernalContext ctx, final GridRestProtocolHandler hnd) {
        super(ctx, hnd);
    }

    /** {@inheritDoc} */
    @Override public Collection<GridRedisCommand> supportedCommands() {
        return SUPPORTED_COMMANDS;
    }

    /** {@inheritDoc} */
    @Override public GridRestRequest asRestRequest(GridRedisMessage msg) throws IgniteCheckedException {
        assert msg != null;

        if (msg.getMsgParts().size() <= VAL_POS)
            throw new GridRedisGenericException("Wrong number of arguments!");

        GridRestCacheRequest appendReq = new GridRestCacheRequest();
        GridRestCacheRequest getReq = new GridRestCacheRequest();

        String val = msg.getMsgParts().get(VAL_POS);

        appendReq.clientId(msg.clientId());
        appendReq.key(msg.key());
        appendReq.value(val);
        appendReq.command(CACHE_APPEND);

        if ((boolean)hnd.handle(appendReq).getResponse() == false) {
            // append on on-existing key in REST returns false.
            GridRestCacheRequest setReq = new GridRestCacheRequest();

            setReq.clientId(msg.clientId());
            setReq.key(msg.key());
            setReq.value(val);
            setReq.command(CACHE_PUT);

            hnd.handle(setReq);
        }

        getReq.clientId(msg.clientId());
        getReq.key(msg.key());
        getReq.command(CACHE_GET);

        return getReq;
    }

    /** {@inheritDoc} */
    @Override public ByteBuffer makeResponse(final GridRestResponse restRes) {
        if (restRes.getResponse() == null)
            return GridRedisProtocolParser.nil();
        else {
            int resLen = ((String)restRes.getResponse()).length();
            return GridRedisProtocolParser.toInteger(String.valueOf(resLen));
        }
    }
}