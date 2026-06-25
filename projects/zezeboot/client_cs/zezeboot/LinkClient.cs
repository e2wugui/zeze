using Zeze;
using Zeze.Net;
using Zeze.Services;
using Zeze.Util;
using zezeboot.link;
using zezeboot.login;

namespace zezeboot
{
    public class LinkClient : HandshakeClient
    {
        public static readonly LinkClient Instance = new LinkClient();

        private LinkClient() : base("LinkClient", new Config())
        {
            ModuleLinkdBase.Instance.Register(this);
            ModuleLink.Instance.Register(this);
            ModuleLogin.Instance.Register(this);

            Config.HandshakeOptions.LoadRsaPubKey(Str.toBytes(
                "8D765D6ADFC55025432EF88C1016548054BCBB33F3DDB624096E83125CD4ABFE" +
                "810FFBCCBBAE3814CE6601FB3ABA92ED9BB781EE34F8E89E8D5D1FECBE8E41B5" +
                "4A271F5EE330AA0B774414ADD6EF312D713726877C4F1CD4C9636CC2E2E03939" +
                "94AA3AD6900C578C4A811B88FFED27CED3F1B9E19F7E99D7797663D3D0C36623" +
                "DE93261F322267770427799EA38149EDD823834F4D015DE7E10367055DFC1868" +
                "5BD29D8DA6E840483180B25D65CEC6E7C03927A83CB2191238564C55490D1920" +
                "80B2D4C52EC6E917E21E297CCC6C119A51A6196CD8D234B0F75425B6BDD25968" +
                "EF3427CAA7BB156C57DF62D5CCA7EE22E44BAF8A99417F6367CA6B37CABB47AF"));
        }

        public override void OnHandshakeDone(AsyncSocket sender)
        {
            base.OnHandshakeDone(sender);
            ModuleLink.Instance.OnHandshakeDone(sender);
        }
    }
}
