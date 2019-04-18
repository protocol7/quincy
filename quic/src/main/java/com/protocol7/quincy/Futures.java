package com.protocol7.quincy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.util.function.Function;

public class Futures {

  public static <V, T> Future<T> thenSync(final Future<V> future, final Function<V, T> f) {
    final DefaultPromise<T> result = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

    future.addListener(
        new GenericFutureListener<Future<V>>() {
          @Override
          public void operationComplete(final Future<V> future) {
            result.setSuccess(f.apply(future.getNow())); // TODO handle exception
          }
        });

    return result;
  }

  public static <V, T> Future<T> thenAsync(final Future<V> future, final Function<V, Future<T>> f) {
    final DefaultPromise<T> result = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

    future.addListener(
        new GenericFutureListener<Future<V>>() {
          @Override
          public void operationComplete(final Future<V> future) throws Exception {
            Futures.thenSync(
                f.apply(future.get()),
                (Function<T, Void>)
                    t -> {
                      result.setSuccess(t); // TODO handle exception
                      return null;
                    });
          }
        });

    return result;
  }

  public static Future<Channel> thenChannel(final ChannelFuture future) {
    return thenSync(future, aVoid -> future.channel());
  }
}
