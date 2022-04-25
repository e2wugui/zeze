
using System;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Zeze.Transaction;
using Zeze.Util;

namespace Arch
{
	[TestClass]
	public class TestRedirectRank
	{
		[TestMethod]
		public async Task TestRedirect()
		{
			var app1 = Game.App.Instance;
			var app2 = new Game.App();

			app1.Start(new string[] { "-ServerId", "0" });
			app2.Start(new string[] { "-ServerId", "1", "-ProviderDirectPort", "20002" });

			Console.WriteLine("Begin Thread.sleep");
			await Task.Delay(2000); // wait connected
			var app1subs = new StringBuilder();
			Str.BuildString(app1subs, app1.Zeze.ServiceManagerAgent.SubscribeStates.Values);
			Console.WriteLine("End Thread.sleep app1 " + app1subs.ToString());
			var app2subs = new StringBuilder();
			Str.BuildString(app2subs, app2.Zeze.ServiceManagerAgent.SubscribeStates.Values);
			Console.WriteLine("End Thread.sleep app2 " + app2subs.ToString());

			try
			{
				int outParam = 0;
				int outServerId = 0;

				// RedirectToServer
				await app1.Game_Rank.TestToServer(0, 111, (i, s) => { outParam = i; outServerId = s; });
				Assert.IsTrue(outParam == 111);
				Assert.IsTrue(outServerId == 0);
				await app1.Game_Rank.TestToServer(1, 222, (i, s) => { outParam = i; outServerId = s; });
				Assert.IsTrue(outParam == 222);
				Assert.IsTrue(outServerId == 1);

				await app2.Game_Rank.TestToServer(0, 333, (i, s) => { outParam = i; outServerId = s; });
				Assert.IsTrue(outParam == 333);
				Assert.IsTrue(outServerId == 0);

				await app2.Game_Rank.TestToServer(1, 444, (i, s) => { outParam = i; outServerId = s; });
				Assert.IsTrue(outParam == 444);
				Assert.IsTrue(outServerId == 1);

				// RedirectHash
				await app1.Game_Rank.TestHash(0, 555, (i, s) => { outParam = i; outServerId = s; });
				Assert.IsTrue(outParam == 555);
				Assert.IsTrue(outServerId == 0);

				await app1.Game_Rank.TestHash(1, 666, (i, s) => { outParam = i; outServerId = s; });
				Assert.IsTrue(outParam == 666);
				Assert.IsTrue(outServerId == 1);

				await app2.Game_Rank.TestHash(0, 777, (i, s) => { outParam = i; outServerId = s; });
				Assert.IsTrue(outParam == 777);
				Assert.IsTrue(outServerId == 0);

				await app2.Game_Rank.TestHash(1, 888, (i, s) => { outParam = i; outServerId = s; });
				Assert.IsTrue(outParam == 888);
				Assert.IsTrue(outServerId == 1);

				// RedirectToServerResult
				long result = 0;
				result = await app1.Game_Rank.TestToServerResult(0, 111, (i, s) => { outParam = i; outServerId = s; });
				Assert.AreEqual(12345, result);
				Assert.IsTrue(outParam == 111);
				Assert.IsTrue(outServerId == 0);
				result = await app1.Game_Rank.TestToServerResult(1, 222, (i, s) => { outParam = i; outServerId = s; });
				Assert.AreEqual(12345, result);
				Assert.IsTrue(outParam == 222);
				Assert.IsTrue(outServerId == 1);

				result = await app2.Game_Rank.TestToServerResult(0, 333, (i, s) => { outParam = i; outServerId = s; });
				Assert.AreEqual(12345, result);
				Assert.IsTrue(outParam == 333);
				Assert.IsTrue(outServerId == 0);

				result = await app2.Game_Rank.TestToServerResult(1, 444, (i, s) => { outParam = i; outServerId = s; });
				Assert.AreEqual(12345, result);
				Assert.IsTrue(outParam == 444);
				Assert.IsTrue(outServerId == 1);

				// RedirectHashResult
				result = await app1.Game_Rank.TestHashResult(0, 555, (i, s) => { outParam = i; outServerId = s; });
				Assert.AreEqual(12345, result);
				Assert.IsTrue(outParam == 555);
				Assert.IsTrue(outServerId == 0);

				result = await app1.Game_Rank.TestHashResult(1, 666, (i, s) => { outParam = i; outServerId = s; });
				Assert.AreEqual(12345, result);
				Assert.IsTrue(outParam == 666);
				Assert.IsTrue(outServerId == 1);

				result = await app2.Game_Rank.TestHashResult(0, 777, (i, s) => { outParam = i; outServerId = s; });
				Assert.AreEqual(12345, result);
				Assert.IsTrue(outParam == 777);
				Assert.IsTrue(outServerId == 0);

				result = await app2.Game_Rank.TestHashResult(1, 888, (i, s) => { outParam = i; outServerId = s; });
				Assert.AreEqual(12345, result);
				Assert.IsTrue(outParam == 888);
				Assert.IsTrue(outServerId == 1);

                // RedirectAll
                {
					var param = 1;
					var ctx = await app1.Game_Rank.TestAllResult(param);
					Assert.AreEqual(0, ctx.HashCodes.Count);
					Assert.AreEqual(0, ctx.HashErrors.Count);
					Assert.AreEqual(100, ctx.HashResults.Count);
					for (int hash = 0; hash < 100; hash++)
                    {
						Assert.IsTrue(ctx.HashResults.ContainsKey(hash));
						Assert.IsTrue(ctx.HashResults.ContainsValue(hash<<32|param));
					}
				}
				/*
				app1.Game_Rank.TestToAllConcLevel = 6;
				var future1 = new TaskCompletionSource<Boolean>();
				var hashes = new ConcurrentHashSet<Integer>();
				app1.Game_Rank.TestToAll(12345, ctx-> {
					assertFalse(ctx.isTimeout());
					var lastResult = ctx.getLastResult();
					var h = lastResult.getHash();
					var out = lastResult.out;
					System.out.println("TestToAll onResult: " + lastResult.getSessionId() + ", " + h + ", " + out);
					assertTrue(h >= 0 && h < app1.Game_Rank.TestToAllConcLevel);
					assertTrue(hashes.add(h));
					if (lastResult.getResultCode() == Procedure.Success)
						assertEquals(12345, out);
					else if (lastResult.getResultCode() == Procedure.Exception)
						assertEquals(0, out);
					if (ctx.isCompleted())
					{
						try
						{
							var allResults = ctx.getAllResults();
							System.out.println("TestToAll onHashEnd: HashResults=" + allResults);
							assertEquals(app1.Game_Rank.TestToAllConcLevel, allResults.size());
							assertEquals(Procedure.Success, allResults.get(0).getResultCode()); // local
							assertEquals(Procedure.Success, allResults.get(1).getResultCode()); // remote
							assertEquals(Procedure.Exception, allResults.get(2).getResultCode()); // local exception
							assertEquals(Procedure.Exception, allResults.get(3).getResultCode()); // remote exception
							assertEquals(Procedure.Success, allResults.get(4).getResultCode()); // local async
							assertEquals(Procedure.Success, allResults.get(4).getResultCode()); // remote async
						}
						finally
						{
							future1.SetResult(true);
						}
					}
				});
				assertTrue(future1.get());
				assertEquals(app1.Game_Rank.TestToAllConcLevel, hashes.size());

				var future2 = new TaskCompletionSource<Boolean>();
				app2.Game_Rank.TestToAllConcLevel = 0;
				app2.Game_Rank.TestToAll(12345, ctx-> {
					if (ctx.isCompleted())
					{
						System.out.println("TestToAll onHashEnd: HashResults=" + ctx.getAllResults());
						assertEquals(0, ctx.getAllResults().size());
						future2.SetResult(true);
					}
				});
				assertTrue(future2.get());
				*/
			}
			finally
			{
				Console.WriteLine("Begin Stop");
				app1.Stop();
				app2.Stop();
				Console.WriteLine("End Stop");
			}
		}
	}
}
