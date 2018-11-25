package com.protocol7.nettyquick.utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.*;

import java.util.function.Function;

public class Futures {

  public static <V, T> Future<T> thenSync(Future<V> future, Function<V, T> f) {
    DefaultPromise<T> result = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

    future.addListener(new GenericFutureListener<Future<V>>() {
      @Override
      public void operationComplete(final Future<V> future) throws Exception {
        result.setSuccess(f.apply(future.getNow())); // TODO handle exception
      }
    });

    return result;
  }

  public static <V, T> Future<T> thenAsync(Future<V> future, Function<V, Future<T>> f) {
    DefaultPromise<T> result = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

    future.addListener(new GenericFutureListener<Future<V>>() {
      @Override
      public void operationComplete(final Future<V> future) throws Exception {
        Futures.thenSync(f.apply(future.get()), (Function<T, Void>) t -> {
          result.setSuccess(t); // TODO handle exception
          return null;
        });
      }
    });

    return result;
  }

  public static Future<Channel> thenChannel(ChannelFuture future) {
    return thenSync(future, aVoid -> future.channel());
  }
}
