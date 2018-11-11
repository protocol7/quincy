package com.protocol7.nettyquick.utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.function.Function;

public class Futures {

  public static <V, T> Future<T> thenSync(EventExecutor executor, Future<V> future, Function<V, T> f) {
    DefaultPromise<T> result = new DefaultPromise<>(executor);

    future.addListener(new GenericFutureListener<Future<V>>() {
      @Override
      public void operationComplete(final Future<V> future) throws Exception {
        result.setSuccess(f.apply(future.getNow())); // TODO handle exception
      }
    });

    return result;
  }

  public static <V, T> Future<T> thenAsync(EventExecutor executor, Future<V> future, Function<V, Future<T>> f) {
    DefaultPromise<T> result = new DefaultPromise<>(executor);

    future.addListener(new GenericFutureListener<Future<V>>() {
      @Override
      public void operationComplete(final Future<V> future) throws Exception {
        Futures.thenSync(executor, f.apply(future.get()), (Function<T, Void>) t -> {
          result.setSuccess(t); // TODO handle exception
          return null;
        });
      }
    });

    return result;
  }

  public static Future<Channel> thenChannel(EventExecutor executor, ChannelFuture future) {
    return thenSync(executor, future, aVoid -> future.channel());
  }
}
