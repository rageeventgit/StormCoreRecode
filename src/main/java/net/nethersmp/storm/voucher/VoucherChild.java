package net.nethersmp.storm.voucher;

import net.nethersmp.storm.voucher.api.VoucherCommand;
import net.nethersmp.storm.voucher.api.VoucherId;
import net.nethersmp.storm.voucher.api.VoucherSettings;

public record VoucherChild<T>(VoucherId id, T worth, VoucherSettings settings, VoucherCommand command) {


}
