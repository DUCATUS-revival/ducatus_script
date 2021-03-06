package io.lastwill.eventscan.services.scheduler;

import com.neemre.btcdcli4j.core.BitcoindException;
import com.neemre.btcdcli4j.core.CommunicationException;
import com.neemre.btcdcli4j.core.client.BtcdClient;
import com.neemre.btcdcli4j.core.domain.AddressBalance;
import io.lastwill.eventscan.model.DucatusTransitionCli;
import io.lastwill.eventscan.repositories.DucatusTransitionCliRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;

@Slf4j
@Component
public class FillBalanceCli {
    @Autowired
    private DucatusTransitionCliRepository repository;

    @Autowired
    private BtcdClient client;

    public void fillBalances() {
        List<DucatusTransitionCli> entries = repository.findAllByAmountIsNull();
        if (entries == null) {
            return;
        }
        for (DucatusTransitionCli e : entries) {
            String address = e.getAddress();
            BigInteger amount = null;
            try {
                AddressBalance addressBalance = client.getAddressBalance(address);
                if (addressBalance == null) {
                    log.warn("address balance is null on address {}", address);
                    continue;
                }
                amount = addressBalance.getBalance();
            } catch (BitcoindException ex) {
                log.error("BitcoinD exception");
                if (ex.getCode() == -5) {
                    continue;
                }
                ex.printStackTrace();
            } catch (CommunicationException ex) {
                ex.printStackTrace();
                continue;
            }
            if (amount != null) {
                e.setAmount(amount);
                repository.save(e);
                log.info("on address {} balance = {}. Save!", address, amount);
            } else {
                log.warn("Can't read balance by address {}, balacne is null", address);
            }
        }
        log.debug("FILL ADDRESSES BY BALANCES COMPLETED");
    }
}
