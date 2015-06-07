/*
 * Copyright 2015, by Vladimir Kostyukov and Contributors.
 *
 * This file is a part of a Finch library that may be found at
 *
 *      https://github.com/finagle/finch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributor(s): -
 */

package io.finch

import com.twitter.finagle.Service
import shapeless.HNil

/**
 * This package contains various of functions and types that enable _router combinators_ in Finch. A Finch
 * [[io.finch.route.Router Router]] is an abstraction that is responsible for routing the HTTP requests using their
 * method and path information. There are two types of routers in Finch: [[io.finch.route.Router0 Router0]] and
 * [[io.finch.route.RouterN RouterN]]. `Router0` matches the route and returns an `Option` of the rest of the route.
 * `RouterN[A]` (or just `Router[A]`) in addition to the `Router0` behaviour extracts a value of type `A` from the
 * route.
 *
 * A [[io.finch.route.Router Router]] that maps route to a [[com.twitter.finagle.Service Service]] is called an
 * [[io.finch.route.Endpoint Endpoint]]. An endpoint `Req => Rep` might be implicitly converted into a
 * `Service[Req, Rep]`. Thus, the following example is a valid Finch code:
 *
 * {{{
 *   def hello(s: String) = new Service[HttRequest, HttpResponse] {
 *     def apply(req: HttpRequest) = Ok(s"Hello $name!").toFuture
 *   }
 *
 *   Httpx.serve(
 *     new InetSocketAddress(8081),
 *     Get / string /> hello // will be implicitly converted into service
 *   )
 * }}}
 */
package object route {
  import tokens._

  private[route] type Route = List[RouteToken]

  /**
   * Converts `Req` to `Route`.
   */
  private[finch] def requestToRoute[Req](req: HttpRequest): Route =
    (MethodToken(req.method): RouteToken) :: (req.path.split("/").toList.drop(1) map PathToken)

  /**
   * A user friendly alias for [[io.finch.route.RouterN RouterN]].
   */
  type Router[A] = RouterN[A]

  /**
   * An alias for [[io.finch.route.Router Router]] that maps route to a [[com.twitter.finagle.Service Service]].
   */
  type Endpoint[A, B] = Router[Service[A, B]]

  implicit def intToMatcher(i: Int): Router0 = new Matcher(i.toString)
  implicit def stringToMatcher(s: String): Router0 = new Matcher(s)
  implicit def booleanToMatcher(b: Boolean): Router0 = new Matcher(b.toString)

  private[route] def stringToSomeValue[A](fn: String => A)(s: String): Option[A] =
    try Some(fn(s)) catch { case _: IllegalArgumentException => None }

  /**
   * Add `/>` compositors to `RouterN` to compose it with function of one argument.
   */
  implicit class RArrow0[R](r: R)(implicit ev: R => RouterN[HNil]) {
    def />[B](v: => B): RouterN[B] = r.map(_ => v)
  }
}

package route {
  /**
   * An exception, which is thrown by router in case of missing route `r`.
   */
  case class RouteNotFound(r: String) extends Exception(s"Route not found: $r")
}
