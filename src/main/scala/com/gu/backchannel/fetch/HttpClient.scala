package com.gu.backchannel.fetch

import _root_.dispatch.thread.ThreadSafeHttpClient
import _root_.dispatch.url._
import org.apache.http.params.HttpParams
import org.apache.http.conn.params.ConnRouteParams
import dispatch.{url, Http, Request, thread}

object HttpClient {

  def get(uri: String, params: Traversable[(String, String)] = Map()): String = apply(url(uri) <<? params )

  def apply(request: Request) = {
    println("Sending GET " + request)
    ThreadSafeHttp(request as_str)
  }

  object ThreadSafeHttp extends Http with thread.Safety{
    // This is there to NOT go through the proxy, as that won't recognise localhost addresses
    override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections = 50, maxConnectionsPerRoute = 50) {
      override protected def configureProxy(params: HttpParams) = {
        ConnRouteParams.setDefaultProxy(params, null)
        params
      }
    }
  }
}