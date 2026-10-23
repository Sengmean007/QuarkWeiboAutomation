package org.sengmean.support;

/**
 * The member-area pages behind the account sidebar - confirmed live against
 * test-v2.138hk.vip on 2026-10-22, signed in as the shared QA account ({@code qa00002}), by
 * clicking every sidebar entry in turn and reading back {@code location.href}.
 *
 * <h2>Where "Transfer" went</h2>
 * The header's own "Transfer" shortcut (next to Deposit/Withdraw/Rebate) is not a distinct page -
 * it opens {@link #WALLET} directly to the From/To/Amount form already on that page, confirmed by
 * clicking it and reading back the same {@code /en/member/wallet/} URL {@link #WALLET} itself
 * resolves to. So this enum has ten entries, not eleven.
 *
 * <h2>Confirms this project's existing page objects</h2>
 * {@link #DEPOSIT} and {@link #WITHDRAWAL} match {@code DepositPage.PATH} and
 * {@code WithdrawalPage.PATH} exactly, and {@link #WALLET} matches
 * {@code TransferPage.WALLET_PATH} - independent confirmation that those three paths are right.
 */
public enum MemberPage {

    PERSONAL_INFORMATION("Personal Information", "/en/member/account/"),
    PASSWORD("Password", "/en/member/change-password/"),
    WALLET("Wallet", "/en/member/wallet/"),
    DEPOSIT("Deposit", "/en/member/deposit/"),
    WITHDRAWAL("Withdrawal", "/en/member/withdrawal/"),
    REBATE_CLAIM("Rebate claim", "/en/member/rebate-claim/"),
    REFERRAL_LINK("Referral link", "/en/member/referral-link/"),
    REFERRAL_BONUS("Referral bonus", "/en/member/referral-bonus/"),
    MEMBERSHIP_PROGRAM("Membership program", "/en/member/membership-program/"),
    RECORDS("Records", "/en/member/record/");

    /** The sidebar entry's own visible label. */
    public final String label;

    /** Path relative to {@code BasePage.BASE_URL}, e.g. "/en/member/wallet/". */
    public final String path;

    MemberPage(String label, String path) {
        this.label = label;
        this.path = path;
    }

    @Override
    public String toString() {
        return label;
    }
}
