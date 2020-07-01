package me.amanj.greenish.checker

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ ImplicitSender, TestKit }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class CommandRunnerSpec()
    extends TestKit(ActorSystem("CommandRunnerSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Run command" must {

    "send back false, when command does not exit" in {
      val actor = system.actorOf(Props(new CommandRunner()))
      actor ! Run("a;kjdw", Seq.empty)
      expectMsg(false)
    }

    "send back false, when command does not exit with 0" in {
      val actor = system.actorOf(Props(new CommandRunner()))
      actor ! Run("false", Seq.empty)
      expectMsg(false)
    }

    "send back true, when command does exit with 0" in {
      val actor = system.actorOf(Props(new CommandRunner()))
      actor ! Run("true", Seq.empty)
      expectMsg(true)
    }

    "use provided environment variables" in {
      val scriptPath = getClass.getResource("/test-env").getFile
      val actor = system.actorOf(Props(new CommandRunner()))
      actor ! Run(scriptPath, Seq.empty)
      expectMsg(false)

      actor ! Run(scriptPath, Seq("GREENISH_VALUE_FOR_TEST" -> "."))
      expectMsg(true)
    }
  }
}
