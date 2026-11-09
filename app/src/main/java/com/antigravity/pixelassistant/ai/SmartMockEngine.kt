package com.antigravity.pixelassistant.ai

data class ReplyCandidate(
    val id: String,
    val label: String,
    val text: String
)

/**
 * Contextual smart mock engine that generates 4 natural reply options without an API key.
 * Analyzes topics (movie, food, weekend, greetings) in Japanese and English.
 */
object SmartMockEngine {

    fun generateReplyCandidates(partnerText: String): List<ReplyCandidate> {
        val clean = partnerText.trim()
        val isEnglish = clean.isNotEmpty() && !clean.any { it in '\u3040'..'\u30ff' || it in '\u4e00'..'\u9faf' }

        if (isEnglish) {
            return listOf(
                ReplyCandidate("1", "😊 Warm + Question", "That sounds awesome! How did you get into that recently? 😊"),
                ReplyCandidate("2", "✨ Friendly", "Haha I totally relate! How's your week going so far?"),
                ReplyCandidate("3", "😄 Playful", "Okay, now you definitely have to tell me the full story behind that 😄"),
                ReplyCandidate("4", "☕ Hangout", "We should totally grab coffee or drinks sometime this week and chat more about it! ☕")
            )
        }

        // Japanese topic matching
        if (clean.contains(Regex("映画|ドラマ|ネトフリ|Netflix|アニメ", RegexOption.IGNORE_CASE))) {
            return listOf(
                ReplyCandidate("1", "😊 共感＋質問", "いいですね！最近観た中で一番おすすめの作品ってどれですか？🎬"),
                ReplyCandidate("2", "✨ フレンドリー", "私も映像作品見るの好きです！休日はイッキ見しちゃうタイプですか？😊"),
                ReplyCandidate("3", "😄 ユーモア", "それ気になりすぎて夜更かし確定しちゃうやつですね😂"),
                ReplyCandidate("4", "☕ お誘い・提案", "今度美味しいご飯行きがてら、おすすめ作品プレゼン大会しましょう☕✨")
            )
        }

        if (clean.contains(Regex("ご飯|カフェ|美味しい|ラーメン|焼肉|イタリアン|飲み|お酒|ランチ|ディナー"))) {
            return listOf(
                ReplyCandidate("1", "😊 共感＋質問", "めちゃくちゃ美味しそうですね！そのお店よく行かれるんですか？😋"),
                ReplyCandidate("2", "✨ フレンドリー", "美味しいもの食べてる時が一番幸せですよね✨ 好きなジャンルとかありますか？"),
                ReplyCandidate("3", "😄 ユーモア", "飯テロすぎます…！今すぐお腹鳴りました😂"),
                ReplyCandidate("4", "☕ お誘い・提案", "もしよかったら今度一緒にそのお店（か気になるところ）行きませんか？🍽️")
            )
        }

        if (clean.contains(Regex("休み|休日|週末|土日|予定"))) {
            return listOf(
                ReplyCandidate("1", "😊 共感＋質問", "いい休日の過ごし方ですね！普段はインドア派ですか？お出かけ派ですか？😊"),
                ReplyCandidate("2", "✨ フレンドリー", "リフレッシュできそうで素敵です✨ 今週末もどこかお出かけ予定ですか？"),
                ReplyCandidate("3", "😄 ユーモア", "充実度がすごいです！私の休日にもそのアクティブさ分けてほしいです😂"),
                ReplyCandidate("4", "☕ お誘い・提案", "タイミング合えば、今週か来週あたり軽くお茶でも行きませんか？☕")
            )
        }

        // Versatile general options
        val snippet = if (clean.length > 15) "「${clean.take(15)}…」" else if (clean.isNotEmpty()) "「$clean」" else ""
        return listOf(
            ReplyCandidate("1", "😊 共感＋質問", "${if (snippet.isNotEmpty()) "$snippet、いいですね！" else "メッセージありがとうございます！"}最近一番ハマってることって何ですか？😊"),
            ReplyCandidate("2", "✨ フレンドリー", "すごくわかります！お話しやすそうで嬉しいです✨ 普段はどんなお仕事されてるんですか？"),
            ReplyCandidate("3", "😄 ユーモア", "センス良すぎます😂 もっと詳しく聞きたいのでぜひ教えてください！"),
            ReplyCandidate("4", "☕ お誘い・提案", "お話が合いそうなので、よかったら今度気軽にお茶でもしながらお話ししませんか？☕")
        )
    }

    fun translateSubtitles(englishText: String): String {
        val trimmed = englishText.trim()
        if (trimmed.isEmpty()) return ""
        // Common subtitle phrase fallbacks
        return when {
            trimmed.contains("hello", true) -> "こんにちは"
            trimmed.contains("thank", true) -> "ありがとう"
            trimmed.contains("let's go", true) -> "行こう！"
            trimmed.contains("what are you doing", true) -> "何をしているんだ？"
            trimmed.contains("I love you", true) -> "愛してるよ"
            trimmed.contains("are you ready", true) -> "準備はいいかい？"
            else -> "「$trimmed」"
        }
    }
}
